package fr.talosi.formation.support.service;

import fr.talosi.formation.support.model.TicketAnalysis;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

@Service
public class OllamaTicketAnalysisService implements TicketAnalysisService {

    private static final String SYSTEM_PROMPT = """
            Tu analyses un ticket de support et tu produis uniquement la structure demandée.

            Règles impératives :
            - Résume uniquement les faits explicitement présents dans le ticket.
            - N'invente jamais de cause, de diagnostic, de personne, de date ou d'action réalisée.
            - Si une cause n'est pas écrite dans le ticket, ne l'ajoute pas au résumé.
            - Choisis LOW, MEDIUM ou HIGH selon l'impact et l'urgence décrits.
            - Si l'impact ou l'urgence ne peuvent pas être évalués, utilise MEDIUM et indique
              les précisions nécessaires dans missingInformation.
            - missingInformation contient uniquement les questions utiles pour poursuivre
              le diagnostic. Utilise une liste vide si le ticket est suffisamment complet.
            - Ne place aucune information hors des champs du type de sortie.
            """;

    private final ChatClient chatClient;

    public OllamaTicketAnalysisService(ChatClient.Builder chatClientBuilder) {
        this.chatClient = chatClientBuilder.build();
    }

    @Override
    public TicketAnalysis analyze(String ticketText) {
        try {
            TicketAnalysis result = chatClient.prompt()
                    .system(SYSTEM_PROMPT)
                    .user("""
                            Analyse le ticket délimité ci-dessous.

                            --- DÉBUT DU TICKET ---
                            %s
                            --- FIN DU TICKET ---
                            """.formatted(ticketText.trim()))
                    .call()
                    .entity(TicketAnalysis.class);

            if (result == null) {
                throw new TicketAnalysisException("Le modèle n'a retourné aucune analyse");
            }
            return result;
        } catch (TicketAnalysisException exception) {
            throw exception;
        } catch (RuntimeException exception) {
            throw new TicketAnalysisException(
                    "La réponse du modèle ne peut pas être convertie en TicketAnalysis",
                    exception
            );
        }
    }
}
