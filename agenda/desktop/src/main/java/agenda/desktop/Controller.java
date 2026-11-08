package agenda.desktop;

import agenda.Agenda;
import agenda.Occurrence;

import java.time.LocalDate;
import java.time.LocalDateTime;

/** Ce que les vues demandent à la fenêtre principale. */
interface Controller {

    Agenda agenda();

    LocalDate today();

    LocalDate selected();

    /** Change le jour sélectionné (et la période affichée si besoin). */
    void select(LocalDate day);

    /** Affiche ce jour dans la vue « Jour ». */
    void showDay(LocalDate day);

    /** Ouvre le formulaire de création d'un événement. */
    void create(LocalDateTime start, boolean allDay);

    /** Ouvre le formulaire de modification d'une occurrence. */
    void open(Occurrence occurrence);
}
