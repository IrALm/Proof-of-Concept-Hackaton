package com.example.demo.admin.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Résultat d'un import CSV : compteurs + détail des erreurs éventuelles.
 */
public class ImportResult {

    private int totalParsed;          // Lignes lues depuis le CSV
    private int successCount;         // Lignes insérées en DB
    private int errorCount;           // Lignes en erreur (parsing ou insert)
    private final List<String> errorDetails = new ArrayList<>();

    public void addSuccess(int n) { this.successCount += n; }
    public void addError(int n, String message) {
        this.errorCount += n;
        if (message != null && !message.isBlank()) {
            this.errorDetails.add(message);
        }
    }
    public void addErrorMessage(String message) {
        if (message != null && !message.isBlank()) {
            this.errorDetails.add(message);
        }
    }

    public int getTotalParsed() { return totalParsed; }
    public void setTotalParsed(int totalParsed) { this.totalParsed = totalParsed; }
    public int getSuccessCount() { return successCount; }
    public int getErrorCount() { return errorCount; }
    public List<String> getErrorDetails() { return errorDetails; }
}
