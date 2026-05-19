package parfumery;

public interface Reportable {
    String toReportString();

    default String getReportHeader() {
        return "Отчёт";
    }
}