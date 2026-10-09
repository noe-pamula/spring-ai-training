"use strict";

const $ = (id) => document.getElementById(id);

const statusNames = {
    OPEN: "Ouvert",
    IN_PROGRESS: "En cours",
    RESOLVED: "Résolu"
};

const priorities = {
    LOW: "Basse",
    NORMAL: "Normale",
    HIGH: "Haute"
};

async function api(url, options = {}) {
    const response = await fetch(url, {
        ...options,
        headers: {
            "Content-Type": "application/json",
            ...options.headers
        }
    });

    if (!response.ok) {
        const error = await response.json().catch(() => ({}));
        throw new Error(error.detail || `La requête a échoué (${response.status}).`);
    }

    return response.json();
}

function element(tag, text, className) {
    const node = document.createElement(tag);

    if (text !== undefined) {
        node.textContent = text;
    }

    if (className) {
        node.className = className;
    }

    return node;
}

let loadSequence = 0;

async function loadTickets() {
    const sequence = ++loadSequence;
    $("ticket-error").textContent = "";

    try {
        const params = new URLSearchParams({
            q: $("search").value
        });

        if ($("status").value) {
            params.set("status", $("status").value);
        }

        const tickets = await api(`/api/tickets?${params}`);

        if (sequence !== loadSequence) {
            return;
        }

        $("tickets").replaceChildren();

        if (!tickets.length) {
            $("tickets").textContent = "Aucun ticket trouvé.";
        }

        for (const ticket of tickets) {
            const node = element("article", undefined, "ticket");

            node.append(
                element("h3", `#${ticket.id} · ${ticket.title}`),
                element("p", ticket.description),
                element(
                    "p",
                    `${ticket.requester} · Priorité ${priorities[ticket.priority]} · ${new Date(ticket.createdAt).toLocaleDateString("fr-FR")}`,
                    "meta"
                )
            );

            const select = element("select");
            select.setAttribute("aria-label", `Statut du ticket ${ticket.id}`);

            for (const [value, label] of Object.entries(statusNames)) {
                const option = element("option", label);
                option.value = value;
                select.append(option);
            }

            select.value = ticket.status;
            select.addEventListener("change", async () => {
                select.disabled = true;

                try {
                    await api(`/api/tickets/${ticket.id}/status`, {
                        method: "PATCH",
                        body: JSON.stringify({ status: select.value })
                    });
                    await loadTickets();
                } catch (error) {
                    $("ticket-error").textContent = error.message;
                    select.value = ticket.status;
                } finally {
                    select.disabled = false;
                }
            });

            node.append(select);
            $("tickets").append(node);
        }
    } catch (error) {
        if (sequence === loadSequence) {
            $("ticket-error").textContent = error.message;
        }
    }
}

async function loadArticles() {
    try {
        const articles = await api("/api/articles");
        $("articles").replaceChildren();

        for (const article of articles) {
            const node = element("details");
            node.append(
                element("summary", article.title),
                element("p", article.category, "meta"),
                element("p", article.content)
            );
            $("articles").append(node);
        }
    } catch (error) {
        $("articles").textContent = error.message;
    }
}

$("refresh").addEventListener("click", loadTickets);
$("status").addEventListener("change", loadTickets);

let timer;

$("search").addEventListener("input", () => {
    clearTimeout(timer);
    timer = setTimeout(loadTickets, 250);
});

$("ticket-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const form = event.currentTarget;
    const button = form.querySelector("button");

    button.disabled = true;
    $("create-message").textContent = "";

    try {
        const ticket = await api("/api/tickets", {
            method: "POST",
            body: JSON.stringify(Object.fromEntries(new FormData(form)))
        });

        form.reset();
        $("create-message").textContent = `Ticket #${ticket.id} créé.`;
        await loadTickets();
    } catch (error) {
        $("create-message").textContent = error.message;
    } finally {
        button.disabled = false;
    }
});

function bubble(text, role) {
    const node = element("div", text, `bubble ${role}`);
    $("messages").append(node);
    $("messages").scrollTop = $("messages").scrollHeight;
    return node;
}

function streamChat(message, target) {
    return new Promise((resolve, reject) => {
        const url = `/api/chat/stream?message=${encodeURIComponent(message)}`;
        const events = new EventSource(url);
        let completed = false;

        const content = (event) => JSON.parse(event.data).content;

        events.addEventListener("chunk", (event) => {
            target.textContent += content(event);
            $("messages").scrollTop = $("messages").scrollHeight;
        });

        events.addEventListener("done", () => {
            completed = true;
            events.close();
            resolve();
        });

        events.addEventListener("generation-error", (event) => {
            events.close();
            reject(new Error(content(event) || "La génération de la réponse a échoué."));
        });

        events.onerror = () => {
            events.close();
            if (!completed) {
                reject(new Error("La connexion au flux de réponse a été interrompue."));
            }
        };
    });
}

$("chat-form").addEventListener("submit", async (event) => {
    event.preventDefault();

    const form = event.currentTarget;
    const message = $("message").value.trim();

    if (!message) {
        return;
    }

    const mode = event.submitter?.dataset.mode ?? "sync";
    const buttons = form.querySelectorAll('button[type="submit"]');
    buttons.forEach((button) => button.disabled = true);
    $("chat-error").textContent = "";
    bubble(message, "user");
    const answer = bubble("", "assistant");
    $("message").value = "";

    try {
        if (mode === "stream") {
            await streamChat(message, answer);
        } else {
            const result = await api("/api/chat", {
                method: "POST",
                body: JSON.stringify({ message })
            });
            answer.textContent = result.reply;
            $("messages").scrollTop = $("messages").scrollHeight;
        }
    } catch (error) {
        if (!answer.textContent) {
            answer.remove();
        }
        $("chat-error").textContent = error.message;
    } finally {
        buttons.forEach((button) => button.disabled = false);
    }
});

loadTickets();
loadArticles();
