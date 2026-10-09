package fr.talosi.formation.support.web;

import fr.talosi.formation.support.service.TicketAnalysisException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/** Spring MVC retourne des erreurs ProblemDetail (400, 404, etc.). */
@RestControllerAdvice
public class ApiExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(TicketAnalysisException.class)
    public ResponseEntity<ProblemDetail> handleTicketAnalysis(TicketAnalysisException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_GATEWAY,
                "Le modèle local n'a pas produit une analyse exploitable."
        );
        problem.setTitle("Réponse structurée invalide");
        return ResponseEntity.status(HttpStatus.BAD_GATEWAY).body(problem);
    }
}
