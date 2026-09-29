package com.docestoque.app;

import android.content.SharedPreferences;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ReportsActivity extends AppCompatActivity {
    private DatabaseHelper databaseHelper;
    private TextView tvRecentReports;
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeSelection();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reports);

        databaseHelper = new DatabaseHelper(this);
        tvRecentReports = findViewById(R.id.tvRecentReports);
        Button btnExport = findViewById(R.id.btnReportExport);
        Button btnHistory = findViewById(R.id.btnReportHistory);
        Button btnBack = findViewById(R.id.btnBackProducts);

        applyThemeColors(findViewById(R.id.reportsRoot), findViewById(R.id.tvReportsTitle),
            tvRecentReports, btnExport, btnHistory, btnBack);

        btnExport.setOnClickListener(v -> ReportExporter.showExportDialog(this, databaseHelper.getAllProducts()));
        btnHistory.setOnClickListener(v -> ReportExporter.showHistoryDialog(this, databaseHelper));
        btnBack.setOnClickListener(v -> finish());
    }

    private void applyThemeSelection() {
        SharedPreferences preferences = getSharedPreferences("estoque_settings", MODE_PRIVATE);
        String theme = preferences.getString("theme", "white");
        if ("black".equals(theme)) {
            setTheme(R.style.AppTheme_Black);
        } else if ("amber".equals(theme)) {
            setTheme(R.style.AppTheme_LightGold);
        } else {
            setTheme(R.style.AppTheme_White);
        }
    }

        private void applyThemeColors(View root, TextView title, TextView recent, Button... buttons) {
        String theme = getSharedPreferences("estoque_settings", MODE_PRIVATE)
            .getString("theme", "white");
        boolean dark = "black".equals(theme);
        boolean amber = "amber".equals(theme);
        int background = ContextCompat.getColor(this,
            dark ? R.color.bg_black : amber ? R.color.bg_amber : R.color.bg_white);
        int primary = ContextCompat.getColor(this,
            dark ? R.color.primary_black : amber ? R.color.primary_amber : R.color.primary_green);
        int primaryText = ContextCompat.getColor(this,
            dark ? R.color.text_white : R.color.text_dark);
        int secondaryText = ContextCompat.getColor(this,
            dark ? R.color.text_muted_white : R.color.text_muted_dark);

        root.setBackgroundColor(background);
        title.setTextColor(primaryText);
        recent.setTextColor(secondaryText);
        for (Button button : buttons) {
            button.setBackgroundTintList(ColorStateList.valueOf(primary));
            button.setTextColor(dark || amber
                ? ContextCompat.getColor(this, R.color.text_dark)
                : ContextCompat.getColor(this, R.color.text_white));
        }
        }

    @Override
    protected void onResume() {
        super.onResume();
        if (databaseHelper != null) {
            databaseExecutor.execute(() -> {
                List<Movement> movements = databaseHelper.getRecentMovements(10);
                List<String> mostMoved = databaseHelper.getMostMovedProducts(3);
                mainHandler.post(() -> updateRecentMovements(movements, mostMoved));
            });
        }
    }

    @Override
    protected void onDestroy() {
        databaseExecutor.shutdownNow();
        super.onDestroy();
    }

    private void updateRecentMovements(List<Movement> movements, List<String> mostMoved) {
        StringBuilder recent = new StringBuilder("Entradas e saídas recentes");
        if (movements.isEmpty()) {
            recent.append("\nNenhuma movimentação registrada");
        } else {
            for (Movement movement : movements) {
                recent.append("\n")
                        .append("IN".equals(movement.getType()) ? "Entrada +" : "Saída -")
                        .append(movement.getQuantity()).append(" - ")
                        .append(movement.getProductName());
            }
        }
        recent.append("\n\nMais movimentados: ")
                .append(mostMoved.isEmpty() ? "nenhum" : String.join(", ", mostMoved));
        tvRecentReports.setText(recent.toString());
    }
}
