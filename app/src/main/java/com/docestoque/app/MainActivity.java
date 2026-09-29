package com.docestoque.app;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.Spinner;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {
    private static final String PREFS_NAME = "estoque_settings";
    private static final String KEY_THEME = "theme";

    private RecyclerView recyclerProducts;
    private TextView tvSummary;
    private TextView tvEmpty;
    private FloatingActionButton fabAdd;
    private Button btnSettings;
    private Button btnDeleteSelected;
    private Button btnDuplicateSelected;
    private Button btnEditSelected;
    private Button btnCancelSelection;
    private EditText etSearch;
    private Spinner spinnerFilter;
    private TextView tvDashboardProducts;
    private TextView tvDashboardStock;
    private TextView tvDashboardValue;
    private TextView tvDashboardAlerts;
    private Button btnReports;
    private Button btnEmptyAdd;
    private Button btnPortfolioContact;
    private TextView tvPortfolioNotice;
    private View emptyState;
    private LinearLayout selectionActions;
    private LinearLayout headerLayout;
    private View rootLayout;
    private DatabaseHelper databaseHelper;
    private ProductAdapter adapter;
    private Uri selectedImageUri;
    private AlertDialog activeProductDialog;
    private Set<Long> selectedProductIds = new HashSet<>();
    private List<Product> allProducts = new ArrayList<>();
    private final ExecutorService databaseExecutor = Executors.newSingleThreadExecutor();
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final Runnable filterRunnable = this::applyFilters;

    private final ActivityResultLauncher<Intent> pickImageLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    selectedImageUri = result.getData().getData();
                    if (selectedImageUri != null) {
                        ImageView imageView = activeProductDialog == null
                                ? null : activeProductDialog.findViewById(R.id.imgProduct);
                        if (imageView != null) {
                            imageView.setImageURI(selectedImageUri);
                        }
                        Toast.makeText(this, "Foto selecionada", Toast.LENGTH_SHORT).show();
                    }
                }
            });

    private final ActivityResultLauncher<Intent> takePhotoLauncher =
            registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
                if (result.getResultCode() == RESULT_OK && selectedImageUri != null) {
                    ImageView imageView = activeProductDialog == null
                            ? null : activeProductDialog.findViewById(R.id.imgProduct);
                    if (imageView != null) {
                        imageView.setImageURI(selectedImageUri);
                    }
                    Toast.makeText(this, "Foto tirada com sucesso", Toast.LENGTH_SHORT).show();
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        applyThemeSelection();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        recyclerProducts = findViewById(R.id.recyclerProducts);
        tvSummary = findViewById(R.id.tvSummary);
        tvEmpty = findViewById(R.id.tvEmpty);
        fabAdd = findViewById(R.id.fabAdd);
        btnSettings = findViewById(R.id.btnSettings);
        btnDeleteSelected = findViewById(R.id.btnDeleteSelected);
        btnDuplicateSelected = findViewById(R.id.btnDuplicateSelected);
        btnEditSelected = findViewById(R.id.btnEditSelected);
        btnCancelSelection = findViewById(R.id.btnCancelSelection);
        etSearch = findViewById(R.id.etSearch);
        spinnerFilter = findViewById(R.id.spinnerFilter);
        tvDashboardProducts = findViewById(R.id.tvDashboardProducts);
        tvDashboardStock = findViewById(R.id.tvDashboardStock);
        tvDashboardValue = findViewById(R.id.tvDashboardValue);
        tvDashboardAlerts = findViewById(R.id.tvDashboardAlerts);
        btnReports = findViewById(R.id.btnReports);
        btnEmptyAdd = findViewById(R.id.btnEmptyAdd);
        btnPortfolioContact = findViewById(R.id.btnPortfolioContact);
        tvPortfolioNotice = findViewById(R.id.tvPortfolioNotice);
        emptyState = findViewById(R.id.emptyState);
        selectionActions = findViewById(R.id.selectionActions);
        headerLayout = findViewById(R.id.headerLayout);
        rootLayout = findViewById(R.id.rootLayout);

        databaseHelper = new DatabaseHelper(this);
        recyclerProducts.setLayoutManager(new LinearLayoutManager(this));
        recyclerProducts.setHasFixedSize(true);
        recyclerProducts.setItemAnimator(null);
        adapter = new ProductAdapter(this, new ProductAdapter.OnProductClickListener() {
            @Override
            public void onEdit(Product product) {
                if (BuildConfig.PORTFOLIO_DEMO) {
                    showProductPreview(product);
                } else {
                    openProductDialog(product, false);
                }
            }

            @Override
            public void onDelete(Product product) {
                deleteProduct(product);
            }

            @Override
            public void onLongPress(Product product) {
                longPressSelect(product);
            }

            @Override
            public void onSelectionChanged(Set<Long> selectedIds) {
                selectedProductIds.clear();
                selectedProductIds.addAll(selectedIds);
                updateSelectionUI();
            }
        });
        recyclerProducts.setAdapter(adapter);

        btnSettings.setOnClickListener(v -> openSettingsDialog());
        btnPortfolioContact.setOnClickListener(v -> openContactEmail());
        btnDeleteSelected.setOnClickListener(v -> deleteSelectedProducts());
        btnDuplicateSelected.setOnClickListener(v -> duplicateSelectedProducts());
        btnEditSelected.setOnClickListener(v -> editSingleSelectedProduct());
        btnCancelSelection.setOnClickListener(v -> clearSelection());
        fabAdd.setOnClickListener(v -> openProductDialog(null, false));
        btnReports.setOnClickListener(v -> startActivity(new Intent(this, ReportsActivity.class)));
        btnEmptyAdd.setOnClickListener(v -> openProductDialog(null, false));
        etSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                mainHandler.removeCallbacks(filterRunnable);
                mainHandler.postDelayed(filterRunnable, 120);
            }
            @Override public void afterTextChanged(Editable s) { }
        });
        ArrayAdapter<CharSequence> filterAdapter = ArrayAdapter.createFromResource(this,
                R.array.product_filters, android.R.layout.simple_spinner_item);
        filterAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFilter.setAdapter(filterAdapter);
        spinnerFilter.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                applyFilters();
            }
            @Override public void onNothingSelected(android.widget.AdapterView<?> parent) { }
        });

        applyThemeColors();
        if (BuildConfig.PORTFOLIO_DEMO) {
            tvPortfolioNotice.setVisibility(View.VISIBLE);
            btnPortfolioContact.setVisibility(View.VISIBLE);
            fabAdd.setVisibility(View.GONE);
            btnEmptyAdd.setVisibility(View.GONE);
        }
        updateSelectionUI();
        loadProducts();
    }

    @Override
    protected void onDestroy() {
        if (adapter != null) {
            adapter.shutdown();
        }
        databaseExecutor.shutdownNow();
        super.onDestroy();
    }

    private void applyThemeSelection() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, "white");
        switch (theme) {
            case "black":
                setTheme(R.style.AppTheme_Black);
                break;
            case "amber":
                setTheme(R.style.AppTheme_LightGold);
                break;
            default:
                setTheme(R.style.AppTheme_White);
                break;
        }
    }

    private void applyThemeColors() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String theme = prefs.getString(KEY_THEME, "white");

        int primary;
        int header;
        int background;
        int textPrimary;
        int textSecondary;
        int headerText;

        if ("black".equals(theme)) {
            background = ContextCompat.getColor(this, R.color.bg_black);
            header = ContextCompat.getColor(this, R.color.header_black);
            primary = ContextCompat.getColor(this, R.color.primary_black);
            textPrimary = ContextCompat.getColor(this, R.color.text_white);
            textSecondary = ContextCompat.getColor(this, R.color.text_muted_white);
            headerText = ContextCompat.getColor(this, R.color.text_white);
        } else if ("amber".equals(theme)) {
            background = ContextCompat.getColor(this, R.color.bg_amber);
            header = ContextCompat.getColor(this, R.color.header_amber);
            primary = ContextCompat.getColor(this, R.color.primary_amber);
            textPrimary = ContextCompat.getColor(this, R.color.text_dark);
            textSecondary = ContextCompat.getColor(this, R.color.text_muted_dark);
            headerText = ContextCompat.getColor(this, R.color.text_dark);
        } else {
            background = ContextCompat.getColor(this, R.color.bg_white);
            header = ContextCompat.getColor(this, R.color.header_green);
            primary = ContextCompat.getColor(this, R.color.primary_green);
            textPrimary = ContextCompat.getColor(this, R.color.text_dark);
            textSecondary = ContextCompat.getColor(this, R.color.text_muted_dark);
            headerText = ContextCompat.getColor(this, R.color.text_white);
        }

        int cardBackground = ContextCompat.getColor(this,
                "black".equals(theme) ? R.color.card_black : R.color.card_light);

        if (rootLayout != null) {
            rootLayout.setBackgroundColor(background);
        }
        if (headerLayout != null) {
            headerLayout.setBackgroundColor(header);
        }
        TextView tvTitle = findViewById(R.id.tvTitle);
        TextView tvProductsTitle = findViewById(R.id.tvProductsTitle);
        TextView tvEmptyHint = findViewById(R.id.tvEmptyHint);
        if (tvTitle != null) {
            tvTitle.setTextColor(headerText);
        }
        if (tvProductsTitle != null) {
            tvProductsTitle.setTextColor(textPrimary);
        }
        if (tvEmptyHint != null) {
            tvEmptyHint.setTextColor(textSecondary);
        }
        if (fabAdd != null) {
            fabAdd.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary));
        }
        if (btnSettings != null) {
            btnSettings.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary));
            btnSettings.setTextColor("black".equals(theme) || "amber".equals(theme)
                    ? ContextCompat.getColor(this, R.color.text_dark)
                    : ContextCompat.getColor(this, R.color.text_white));
        }
        if (selectionActions != null) {
            selectionActions.setBackgroundColor(header);
        }
        Button[] actionButtons = {btnDeleteSelected, btnDuplicateSelected, btnEditSelected, btnCancelSelection};
        for (Button actionButton : actionButtons) {
            if (actionButton != null) {
                actionButton.setTextColor("black".equals(theme)
                    || "amber".equals(theme)
                        ? ContextCompat.getColor(this, R.color.text_dark)
                        : ContextCompat.getColor(this, R.color.text_white));
                actionButton.setBackgroundTintList(android.content.res.ColorStateList.valueOf(primary));
            }
        }
        if (tvSummary != null) {
            tvSummary.setTextColor(headerText);
        }
        if (tvEmpty != null) {
            tvEmpty.setTextColor(textSecondary);
        }
        if (tvDashboardProducts != null) {
            tvDashboardProducts.setTextColor(textPrimary);
            tvDashboardStock.setTextColor(textPrimary);
            tvDashboardValue.setTextColor(textPrimary);
            tvDashboardAlerts.setTextColor(textPrimary);
            etSearch.setTextColor(textPrimary);
            etSearch.setHintTextColor(textSecondary);
        }
        if (adapter != null) {
            adapter.setThemeColors(cardBackground, textPrimary, textSecondary,
                    "amber".equals(theme) ? textPrimary : primary);
        }
    }

    private void openSettingsDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String currentTheme = prefs.getString(KEY_THEME, "white");

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_settings, null, false);
        builder.setView(view);

        TextView title = view.findViewById(R.id.settingsTitle);
        Button whiteBtn = view.findViewById(R.id.btnThemeWhite);
        Button blackBtn = view.findViewById(R.id.btnThemeBlack);
        Button amberBtn = view.findViewById(R.id.btnThemeAmber);

        title.setText("Tema");

        whiteBtn.setOnClickListener(v -> saveThemePreference("white"));
        blackBtn.setOnClickListener(v -> saveThemePreference("black"));
        amberBtn.setOnClickListener(v -> saveThemePreference("amber"));

        if ("black".equals(currentTheme)) {
            blackBtn.setAlpha(1f);
        } else if ("amber".equals(currentTheme)) {
            amberBtn.setAlpha(1f);
        } else {
            whiteBtn.setAlpha(1f);
        }

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void saveThemePreference(String theme) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putString(KEY_THEME, theme)
                .apply();
        recreate();
    }

    private void longPressSelect(Product product) {
        if (BuildConfig.PORTFOLIO_DEMO) {
            return;
        }
        if (selectedProductIds.isEmpty()) {
            selectedProductIds.add(product.getId());
            updateSelectionUI();
            adapter.setSelectedProductIds(selectedProductIds);
            Toast.makeText(this, "Modo de seleção ativado", Toast.LENGTH_SHORT).show();
            return;
        }

        if (selectedProductIds.contains(product.getId())) {
            selectedProductIds.remove(product.getId());
        } else {
            selectedProductIds.add(product.getId());
        }
        updateSelectionUI();
        adapter.setSelectedProductIds(selectedProductIds);
    }

    private void clearSelection() {
        selectedProductIds.clear();
        updateSelectionUI();
        adapter.setSelectedProductIds(selectedProductIds);
    }

    private void updateSelectionUI() {
        if (BuildConfig.PORTFOLIO_DEMO) {
            selectionActions.setVisibility(View.GONE);
            return;
        }
        boolean hasSelection = !selectedProductIds.isEmpty();
        selectionActions.setVisibility(hasSelection ? View.VISIBLE : View.GONE);
        btnEditSelected.setEnabled(selectedProductIds.size() == 1);
        btnDuplicateSelected.setEnabled(hasSelection);
        btnDeleteSelected.setEnabled(hasSelection);
    }

    private void openProductDialog(Product productToEdit, boolean duplicateMode) {
        if (BuildConfig.PORTFOLIO_DEMO) {
            openContactEmail();
            return;
        }
        View dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_product, null, false);
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setView(dialogView);

        EditText etName = dialogView.findViewById(R.id.etName);
        EditText etDescription = dialogView.findViewById(R.id.etDescription);
        EditText etLocation = dialogView.findViewById(R.id.etLocation);
        EditText etQuantity = dialogView.findViewById(R.id.etQuantity);
        EditText etCostPrice = dialogView.findViewById(R.id.etCostPrice);
        EditText etSalePrice = dialogView.findViewById(R.id.etSalePrice);
        EditText etMinimumStock = dialogView.findViewById(R.id.etMinimumStock);
        CheckBox cbActive = dialogView.findViewById(R.id.cbActive);
        ImageView imgProduct = dialogView.findViewById(R.id.imgProduct);
        Button btnChoosePhoto = dialogView.findViewById(R.id.btnChoosePhoto);

        if (productToEdit != null) {
            etName.setText(productToEdit.getName());
            etDescription.setText(productToEdit.getDescription());
            etLocation.setText(productToEdit.getLocation());
            etQuantity.setText(String.valueOf(productToEdit.getQuantity()));
            etCostPrice.setText(formatCurrencyInput(productToEdit.getCostPrice()));
            etSalePrice.setText(formatCurrencyInput(productToEdit.getSalePrice()));
            etMinimumStock.setText(String.valueOf(productToEdit.getMinimumStock()));
            cbActive.setChecked(productToEdit.isActive());
            selectedImageUri = productToEdit.getImagePath() != null ? Uri.parse(productToEdit.getImagePath()) : null;
            if (selectedImageUri != null) {
                imgProduct.setImageURI(selectedImageUri);
            }
        }

        etCostPrice.addTextChangedListener(new CurrencyTextWatcher(etCostPrice));
        etSalePrice.addTextChangedListener(new CurrencyTextWatcher(etSalePrice));
        btnChoosePhoto.setOnClickListener(v -> pickImage());
        Button btnTakePhoto = dialogView.findViewById(R.id.btnTakePhoto);
        btnTakePhoto.setOnClickListener(v -> takePhoto());

        if (productToEdit == null) {
            selectedImageUri = null;
        }

        AlertDialog dialog = builder
                .setTitle(productToEdit == null && !duplicateMode ? "Novo produto" : (duplicateMode ? "Duplicar produto" : "Editar produto"))
                .setPositiveButton("Salvar", null)
                .setNegativeButton("Cancelar", (d, which) -> d.dismiss())
                .create();

        dialog.setOnShowListener(d -> {
            Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String name = getTrimmed(etName);
                String description = getTrimmed(etDescription);
                String location = getTrimmed(etLocation);
                String quantityText = getTrimmed(etQuantity);
                String costText = getTrimmed(etCostPrice);
                String saleText = getTrimmed(etSalePrice);
                String minimumStockText = getTrimmed(etMinimumStock);

                if (TextUtils.isEmpty(name)) {
                    etName.setError("Informe o nome");
                    return;
                }
                if (TextUtils.isEmpty(quantityText)) {
                    etQuantity.setError("Informe a quantidade");
                    return;
                }
                if (TextUtils.isEmpty(costText)) {
                    etCostPrice.setError("Informe o preço de custo");
                    return;
                }
                if (TextUtils.isEmpty(saleText)) {
                    etSalePrice.setError("Informe o preço de venda");
                    return;
                }
                if (TextUtils.isEmpty(minimumStockText)) {
                    minimumStockText = "0";
                }

                Product product = productToEdit != null ? productToEdit : new Product();
                if (duplicateMode && productToEdit != null) {
                    product = new Product();
                    product.setId(0);
                }

                product.setName(name);
                product.setDescription(description);
                product.setLocation(location);
                product.setQuantity(Integer.parseInt(quantityText));
                product.setCostPrice(parseCurrencyValue(costText));
                product.setSalePrice(parseCurrencyValue(saleText));
                product.setImagePath(selectedImageUri != null ? selectedImageUri.toString() : null);
                product.setActive(cbActive.isChecked());
                product.setMinimumStock(Integer.parseInt(minimumStockText));
                Product productToSave = product;

                dialog.dismiss();
                databaseExecutor.execute(() -> {
                    if (productToEdit == null || duplicateMode) {
                        databaseHelper.insertProduct(productToSave);
                    } else {
                        databaseHelper.updateProduct(productToSave);
                    }
                    mainHandler.post(() -> {
                        clearSelection();
                        loadProducts();
                    });
                });
            });
        });

        dialog.show();
        activeProductDialog = dialog;
        dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        dialog.setOnDismissListener(d -> activeProductDialog = null);
    }

    private void deleteProduct(Product product) {
        if (BuildConfig.PORTFOLIO_DEMO) {
            openContactEmail();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Excluir produto")
                .setMessage("Deseja realmente excluir \"" + product.getName() + "\"?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    databaseExecutor.execute(() -> {
                        databaseHelper.deleteProduct(product.getId());
                        mainHandler.post(() -> {
                            clearSelection();
                            loadProducts();
                        });
                    });
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void deleteSelectedProducts() {
        if (BuildConfig.PORTFOLIO_DEMO) {
            openContactEmail();
            return;
        }
        if (selectedProductIds.isEmpty()) {
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle("Excluir seleção")
                .setMessage("Deseja apagar os itens selecionados?")
                .setPositiveButton("Sim", (dialog, which) -> {
                    List<Long> ids = new ArrayList<>(selectedProductIds);
                    databaseExecutor.execute(() -> {
                        for (Long id : ids) {
                            databaseHelper.deleteProduct(id);
                        }
                        mainHandler.post(() -> {
                            clearSelection();
                            loadProducts();
                        });
                    });
                })
                .setNegativeButton("Não", null)
                .show();
    }

    private void duplicateSelectedProducts() {
        if (BuildConfig.PORTFOLIO_DEMO) {
            openContactEmail();
            return;
        }
        if (selectedProductIds.isEmpty()) {
            return;
        }

        Set<Long> ids = new HashSet<>(selectedProductIds);
        databaseExecutor.execute(() -> {
            List<Product> products = databaseHelper.getAllProducts();
            for (Product product : products) {
                if (ids.contains(product.getId())) {
                    Product cloned = new Product();
                    cloned.setName(product.getName());
                    cloned.setDescription(product.getDescription());
                    cloned.setLocation(product.getLocation());
                    cloned.setQuantity(product.getQuantity());
                    cloned.setCostPrice(product.getCostPrice());
                    cloned.setSalePrice(product.getSalePrice());
                    cloned.setImagePath(product.getImagePath());
                    cloned.setActive(product.isActive());
                    cloned.setMinimumStock(product.getMinimumStock());
                    databaseHelper.insertProduct(cloned);
                }
            }
            mainHandler.post(() -> {
                clearSelection();
                loadProducts();
                Toast.makeText(this, "Itens duplicados", Toast.LENGTH_SHORT).show();
            });
        });
    }

    private void editSingleSelectedProduct() {
        if (BuildConfig.PORTFOLIO_DEMO) {
            openContactEmail();
            return;
        }
        if (selectedProductIds.size() != 1) {
            return;
        }
        long id = new ArrayList<>(selectedProductIds).get(0);
        databaseExecutor.execute(() -> {
            List<Product> products = databaseHelper.getAllProducts();
            for (Product product : products) {
                if (product.getId() == id) {
                    mainHandler.post(() -> openProductDialog(product, false));
                    return;
                }
            }
        });
    }

    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void takePhoto() {
        ContentValues values = new ContentValues();
        values.put(MediaStore.Images.Media.DISPLAY_NAME, "estoque_" + System.currentTimeMillis() + ".jpg");
        values.put(MediaStore.Images.Media.MIME_TYPE, "image/jpeg");
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            values.put(MediaStore.Images.Media.RELATIVE_PATH, "Pictures/Estoque+");
        }

        selectedImageUri = getContentResolver().insert(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);
        if (selectedImageUri == null) {
            Toast.makeText(this, "Não foi possível abrir a câmera", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        intent.putExtra(MediaStore.EXTRA_OUTPUT, selectedImageUri);
        intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
        takePhotoLauncher.launch(intent);
    }

    private void loadProducts() {
        databaseExecutor.execute(() -> {
            List<Product> products = databaseHelper.getAllProducts();
            if (BuildConfig.PORTFOLIO_DEMO && products.isEmpty()) {
                seedPortfolioProducts();
                products = databaseHelper.getAllProducts();
            }
            List<Product> loadedProducts = products;
            mainHandler.post(() -> {
                allProducts = loadedProducts;
                updateDashboard();
                applyFilters();
            });
        });
    }

    private void seedPortfolioProducts() {
        addPortfolioProduct("Café especial 500 g", "Torra média, grãos selecionados", "Prateleira A1", 18, 12.50, 24.90, 5);
        addPortfolioProduct("Caderno pontilhado A5", "Capa dura, 160 páginas", "Prateleira B2", 7, 18.00, 34.90, 8);
        addPortfolioProduct("Caneca térmica 450 ml", "Aço inoxidável, azul", "Prateleira C1", 0, 29.00, 59.90, 3);
        addPortfolioProduct("Kit de marcadores", "Conjunto com 12 cores", "Prateleira B1", 12, 15.00, 32.50, 4);
        addPortfolioProduct("Agenda semanal", "Capa verde, edição demonstrativa", "Prateleira A2", 3, 22.00, 45.00, 5);
    }

    private void addPortfolioProduct(String name, String description, String location,
                                     int quantity, double costPrice, double salePrice,
                                     int minimumStock) {
        Product product = new Product();
        product.setName(name);
        product.setDescription(description);
        product.setLocation(location);
        product.setQuantity(quantity);
        product.setCostPrice(costPrice);
        product.setSalePrice(salePrice);
        product.setMinimumStock(minimumStock);
        product.setActive(true);
        databaseHelper.insertProduct(product);
    }

    private void showProductPreview(Product product) {
        String message = product.getDescription() + "\n\n"
                + "Local: " + product.getLocation() + "\n"
                + "Estoque: " + product.getQuantity() + " unidades\n"
                + "Estoque mínimo: " + product.getMinimumStock() + "\n"
                + "Preço: " + formatCurrency(product.getSalePrice());
        new AlertDialog.Builder(this)
                .setTitle(product.getName())
                .setMessage(message)
                .setPositiveButton("Conheça a versão completa", (dialog, which) -> openContactEmail())
                .setNegativeButton("Fechar", null)
                .show();
    }

    private void openContactEmail() {
        String email = getString(R.string.contact_email);
        Intent intent = new Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + email));
        intent.putExtra(Intent.EXTRA_SUBJECT, "Quero conhecer o Estoque +");
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException exception) {
            new AlertDialog.Builder(this)
                    .setTitle("Entre em contato")
                    .setMessage(email)
                    .setPositiveButton("Fechar", null)
                    .show();
        }
    }

    private void applyFilters() {
        if (adapter == null || spinnerFilter == null || etSearch == null) {
            return;
        }
        String search = etSearch.getText() == null ? "" : etSearch.getText().toString().trim().toLowerCase(Locale.ROOT);
        int filter = spinnerFilter.getSelectedItemPosition();
        List<Product> filtered = new ArrayList<>();
        for (Product product : allProducts) {
            boolean matchesSearch = search.isEmpty()
                    || product.getName().toLowerCase(Locale.ROOT).contains(search)
                    || (product.getDescription() != null && product.getDescription().toLowerCase(Locale.ROOT).contains(search));
            boolean matchesFilter = filter == 0
                    || (filter == 1 && product.isActive())
                    || (filter == 2 && !product.isActive())
                    || (filter == 3 && product.getQuantity() > 0 && product.getQuantity() <= product.getMinimumStock())
                    || (filter == 4 && product.getQuantity() == 0);
            if (matchesSearch && matchesFilter) {
                filtered.add(product);
            }
        }
        adapter.setProducts(filtered);
        adapter.setSelectedProductIds(selectedProductIds, false);

        if (filtered.isEmpty()) {
            emptyState.setVisibility(View.VISIBLE);
            recyclerProducts.setVisibility(View.GONE);
        } else {
            emptyState.setVisibility(View.GONE);
            recyclerProducts.setVisibility(View.VISIBLE);
        }

        tvSummary.setText(filtered.size() + " de " + allProducts.size() + " produtos");
    }

    private void updateDashboard() {
        int inStock = 0;
        int alerts = 0;
        double totalValue = 0;
        for (Product product : allProducts) {
            if (product.getQuantity() > 0) {
                inStock++;
            }
            if (product.getQuantity() == 0 || (product.getMinimumStock() > 0 && product.getQuantity() <= product.getMinimumStock())) {
                alerts++;
            }
            totalValue += product.getCostPrice() * product.getQuantity();
        }
        tvDashboardProducts.setText("Produtos\n" + allProducts.size());
        tvDashboardStock.setText("Com estoque\n" + inStock);
        tvDashboardValue.setText("Valor do estoque\n" + formatCurrency(totalValue));
        tvDashboardAlerts.setText("Alertas\n" + alerts);

        // Relatórios e movimentações ficam em uma tela própria para preservar o foco nos produtos.
    }

    private void exportProducts() {
        String[] formats = {"PDF formatado", "Excel formatado (.xls)"};
        new AlertDialog.Builder(this)
                .setTitle("Exportar estoque")
                .setItems(formats, (dialog, which) -> {
                    try {
                        if (which == 0) {
                            shareExportFile(createPdfFile(), "application/pdf", "Estoque + PDF");
                        } else {
                            shareExportFile(createExcelFile(), "application/vnd.ms-excel", "Estoque + Excel");
                        }
                    } catch (IOException exception) {
                        Toast.makeText(this, "Não foi possível gerar o arquivo", Toast.LENGTH_LONG).show();
                    }
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private File createExcelFile() throws IOException {
        File file = new File(getCacheDir(), "estoque_mais.xls");
        StringBuilder html = new StringBuilder();
        html.append("<html><head><meta charset=\"UTF-8\"></head><body>")
                .append("<h2 style=\"color:#1B6B5D;font-family:Arial\">Estoque +</h2>")
                .append("<table border=\"1\" cellspacing=\"0\" cellpadding=\"6\" style=\"border-collapse:collapse;font-family:Arial;font-size:10pt\">")
                .append("<tr style=\"background:#1B6B5D;color:white;font-weight:bold\">")
                .append("<th>NOME</th><th>DESCRIÇÃO</th><th>ESTOQUE</th><th>PREÇO CUSTO</th>")
                .append("<th>PREÇO VENDA</th><th>LOCAL</th><th>STATUS</th></tr>");
        boolean alternate = false;
        for (Product product : allProducts) {
            html.append("<tr style=\"background:")
                    .append(alternate ? "#F2F7F5" : "#FFFFFF")
                    .append("\">")
                    .append("<td>").append(htmlValue(product.getName())).append("</td>")
                    .append("<td>").append(htmlValue(product.getDescription())).append("</td>")
                    .append("<td style=\"text-align:right\">").append(product.getQuantity()).append("</td>")
                    .append("<td style=\"text-align:right\">").append(htmlValue(formatCurrency(product.getCostPrice()))).append("</td>")
                    .append("<td style=\"text-align:right\">").append(htmlValue(formatCurrency(product.getSalePrice()))).append("</td>")
                    .append("<td>").append(htmlValue(product.getLocation())).append("</td>")
                    .append("<td style=\"color:")
                    .append(product.isActive() ? "#168A55" : "#C62828")
                    .append("\">").append(product.isActive() ? "ATIVO" : "INATIVO").append("</td></tr>");
            alternate = !alternate;
        }
        html.append("</table></body></html>");
        try (FileOutputStream output = new FileOutputStream(file)) {
            output.write(html.toString().getBytes(java.nio.charset.StandardCharsets.UTF_8));
        }
        return file;
    }

    private String htmlValue(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("\n", " ")
                .replace("\r", " ");
    }

    private File createPdfFile() throws IOException {
        File file = new File(getCacheDir(), "estoque_mais.pdf");
        PdfDocument document = new PdfDocument();
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        int pageNumber = 1;
        PdfDocument.Page page = document.startPage(new PdfDocument.PageInfo.Builder(842, 595, pageNumber).create());
        Canvas canvas = page.getCanvas();
        int y = drawPdfHeader(canvas, paint);

        for (Product product : allProducts) {
            if (y > 545) {
                document.finishPage(page);
                page = document.startPage(new PdfDocument.PageInfo.Builder(842, 595, ++pageNumber).create());
                canvas = page.getCanvas();
                y = drawPdfHeader(canvas, paint);
            }
            drawPdfRow(canvas, paint, product, y);
            y += 30;
        }
        document.finishPage(page);
        try (FileOutputStream output = new FileOutputStream(file)) {
            document.writeTo(output);
        } finally {
            document.close();
        }
        return file;
    }

    private int drawPdfHeader(Canvas canvas, Paint paint) {
        paint.setColor(0xFF1B6B5D);
        paint.setTextSize(18);
        paint.setTypeface(android.graphics.Typeface.DEFAULT_BOLD);
        canvas.drawText("Estoque +", 24, 32, paint);
        paint.setTextSize(8);
        paint.setTypeface(android.graphics.Typeface.DEFAULT);
        paint.setColor(0xFFE8F2EF);
        canvas.drawRect(18, 42, 824, 65, paint);
        paint.setColor(0xFF1B6B5D);
        canvas.drawText("NOME", 24, 57, paint);
        canvas.drawText("DESCRIÇÃO", 145, 57, paint);
        canvas.drawText("ESTOQUE", 330, 57, paint);
        canvas.drawText("PREÇO CUSTO", 395, 57, paint);
        canvas.drawText("PREÇO VENDA", 490, 57, paint);
        canvas.drawText("LOCAL", 590, 57, paint);
        canvas.drawText("STATUS", 710, 57, paint);
        paint.setColor(0xFF202020);
        return 78;
    }

    private void drawPdfRow(Canvas canvas, Paint paint, Product product, int y) {
        paint.setColor((y / 30) % 2 == 0 ? 0xFFFFFFFF : 0xFFF2F7F5);
        canvas.drawRect(18, y - 14, 824, y + 12, paint);
        paint.setColor(0xFF202020);
        paint.setTextSize(8);
        canvas.drawText(limitPdf(product.getName(), 18), 24, y, paint);
        canvas.drawText(limitPdf(product.getDescription(), 27), 145, y, paint);
        canvas.drawText(String.valueOf(product.getQuantity()), 342, y, paint);
        canvas.drawText(formatCurrency(product.getCostPrice()), 395, y, paint);
        canvas.drawText(formatCurrency(product.getSalePrice()), 490, y, paint);
        canvas.drawText(limitPdf(product.getLocation(), 17), 590, y, paint);
        paint.setColor(product.isActive() ? 0xFF168A55 : 0xFFC62828);
        canvas.drawText(product.isActive() ? "ATIVO" : "INATIVO", 710, y, paint);
        paint.setColor(0xFF202020);
    }

    private String limitPdf(String value, int maxLength) {
        if (value == null) {
            return "-";
        }
        String clean = value.replace('\n', ' ').replace('\r', ' ');
        return clean.length() <= maxLength ? clean : clean.substring(0, maxLength - 1) + "…";
    }

    private void shareExportFile(File file, String mimeType, String title) {
        Uri uri = FileProvider.getUriForFile(this, getPackageName() + ".fileprovider", file);
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType(mimeType);
        share.putExtra(Intent.EXTRA_SUBJECT, title);
        share.putExtra(Intent.EXTRA_STREAM, uri);
        share.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        startActivity(Intent.createChooser(share, title));
    }

    private void openMovementHistoryDialog() {
        String[] periods = {"Últimos 7 dias", "Últimos 30 dias", "Últimos 90 dias", "Todo o histórico"};
        new AlertDialog.Builder(this)
                .setTitle("Histórico de movimentações")
                .setItems(periods, (dialog, which) -> {
                    long since = which == 3 ? 0 : System.currentTimeMillis() - (which == 0 ? 7L : which == 1 ? 30L : 90L) * 24L * 60L * 60L * 1000L;
                    List<Movement> movements = databaseHelper.getMovementsSince(since, 100);
                    StringBuilder text = new StringBuilder();
                    if (movements.isEmpty()) {
                        text.append("Nenhuma movimentação no período selecionado.");
                    } else {
                        for (Movement movement : movements) {
                            text.append("IN".equals(movement.getType()) ? "Entrada +" : "Saída -")
                                    .append(movement.getQuantity()).append(" - ")
                                    .append(movement.getProductName()).append('\n');
                        }
                    }
                    new AlertDialog.Builder(this)
                            .setTitle(periods[which])
                            .setMessage(text.toString())
                            .setPositiveButton("Fechar", null)
                            .show();
                })
                .setNegativeButton("Cancelar", null)
                .show();
    }

    private String getTrimmed(EditText editText) {
        return editText.getText() == null ? "" : editText.getText().toString().trim();
    }

    private String formatCurrencyInput(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return format.format(value).replace("R$", "").trim();
    }

    private double parseCurrencyValue(String raw) {
        if (raw == null || raw.trim().isEmpty()) {
            return 0;
        }
        String digits = raw.replaceAll("[^\\d]", "");
        if (digits.isEmpty()) {
            return 0;
        }
        BigDecimal bigDecimal = new BigDecimal(digits);
        return bigDecimal.divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP).doubleValue();
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return format.format(value);
    }

    private static class CurrencyTextWatcher implements TextWatcher {
        private final EditText editText;

        CurrencyTextWatcher(EditText editText) {
            this.editText = editText;
        }

        @Override
        public void beforeTextChanged(CharSequence s, int start, int count, int after) {
        }

        @Override
        public void onTextChanged(CharSequence s, int start, int before, int count) {
        }

        @Override
        public void afterTextChanged(Editable s) {
            if (s == null || s.length() == 0) {
                return;
            }

            String digits = s.toString().replaceAll("[^\\d]", "");
            if (digits.isEmpty()) {
                editText.setText("");
                editText.setSelection(0);
                return;
            }

            long cents = Long.parseLong(digits);
            BigDecimal decimal = BigDecimal.valueOf(cents).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            String formatted = NumberFormat.getCurrencyInstance(new Locale("pt", "BR")).format(decimal);
            String clean = formatted.replace("R$", "").trim();

            if (!clean.equals(s.toString().trim())) {
                editText.setText(clean);
                editText.setSelection(clean.length());
            }
        }
    }
}
