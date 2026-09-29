package com.docestoque.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;
import android.util.LruCache;

import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class ProductAdapter extends RecyclerView.Adapter<ProductAdapter.ProductViewHolder> {
    private final List<Product> products = new ArrayList<>();
    private final Context context;
    private final OnProductClickListener listener;
    private final Set<Long> selectedProductIds = new HashSet<>();
    private int cardBackgroundColor = 0xFFFFFFFF;
    private int primaryTextColor = 0xFF202020;
    private int secondaryTextColor = 0xFF515151;
    private int accentTextColor = 0xFF1B6B5D;
    private final LruCache<String, Bitmap> thumbnailCache = new LruCache<>(24);
    private final ExecutorService imageExecutor = Executors.newFixedThreadPool(2);

    public interface OnProductClickListener {
        void onEdit(Product product);
        void onDelete(Product product);
        void onLongPress(Product product);
        void onSelectionChanged(Set<Long> selectedIds);
    }

    public ProductAdapter(Context context, OnProductClickListener listener) {
        this.context = context;
        this.listener = listener;
    }

    public void setProducts(List<Product> newProducts) {
        products.clear();
        products.addAll(newProducts);
        notifyDataSetChanged();
    }

    public void setSelectedProductIds(Set<Long> ids) {
        setSelectedProductIds(ids, true);
    }

    public void setSelectedProductIds(Set<Long> ids, boolean notify) {
        selectedProductIds.clear();
        selectedProductIds.addAll(ids);
        if (notify) {
            notifyDataSetChanged();
        }
    }

    public void setThemeColors(int cardBackgroundColor, int primaryTextColor,
                               int secondaryTextColor, int accentTextColor) {
        this.cardBackgroundColor = cardBackgroundColor;
        this.primaryTextColor = primaryTextColor;
        this.secondaryTextColor = secondaryTextColor;
        this.accentTextColor = accentTextColor;
        notifyDataSetChanged();
    }

    public void shutdown() {
        imageExecutor.shutdownNow();
    }

    @NonNull
    @Override
    public ProductViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_product, parent, false);
        return new ProductViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ProductViewHolder holder, int position) {
        Product product = products.get(position);
        boolean selected = selectedProductIds.contains(product.getId());

        if (holder.itemView instanceof CardView) {
            ((CardView) holder.itemView).setCardBackgroundColor(cardBackgroundColor);
        }
        holder.tvName.setText(product.getName());
        holder.tvName.setTextColor(primaryTextColor);
        holder.tvDescription.setText(product.getDescription() == null || product.getDescription().trim().isEmpty()
                ? "Sem descrição" : product.getDescription());
        holder.tvDescription.setTextColor(secondaryTextColor);
        holder.tvLocation.setText(product.getLocation() == null || product.getLocation().trim().isEmpty()
                ? "Local: não informado" : "Local: " + product.getLocation());
        holder.tvLocation.setTextColor(secondaryTextColor);
        holder.tvStatus.setText(product.isActive() ? "Ativo" : "Inativo");
        holder.tvStatus.setTextColor(product.isActive() ? accentTextColor : secondaryTextColor);
        holder.tvQuantity.setText("Estoque: " + product.getQuantity() + " unidades");
        if (product.getQuantity() == 0) {
            holder.tvQuantity.setText("Sem estoque");
            holder.tvQuantity.setTextColor(0xFFD32F2F);
        } else if (product.getMinimumStock() > 0 && product.getQuantity() <= product.getMinimumStock()) {
            holder.tvQuantity.setText("Estoque baixo: " + product.getQuantity());
            holder.tvQuantity.setTextColor(0xFFF0A000);
        } else {
            holder.tvQuantity.setTextColor(accentTextColor);
        }
        holder.tvPrice.setText(formatCurrency(product.getSalePrice()));
        holder.tvPrice.setTextColor(accentTextColor);

        String imageKey = product.getImagePath();
        holder.boundImageKey = imageKey;
        holder.imgThumb.setImageResource(android.R.drawable.ic_menu_gallery);
        if (imageKey != null && !imageKey.trim().isEmpty()) {
            Bitmap cachedThumbnail = thumbnailCache.get(imageKey);
            if (cachedThumbnail != null) {
                holder.imgThumb.setImageBitmap(cachedThumbnail);
            } else {
                imageExecutor.execute(() -> {
                    Bitmap thumbnail = getThumbnail(Uri.parse(imageKey));
                    if (thumbnail != null) {
                        holder.itemView.post(() -> {
                            if (imageKey.equals(holder.boundImageKey)) {
                                holder.imgThumb.setImageBitmap(thumbnail);
                            }
                        });
                    }
                });
            }
        } else {
            holder.imgThumb.setImageResource(android.R.drawable.ic_menu_gallery);
        }

        holder.itemView.setBackgroundColor(selected ? 0xFFEFE3B3 : 0x00000000);
        holder.itemView.setAlpha(selected ? 0.9f : 1f);

        holder.itemView.setOnLongClickListener(v -> {
            listener.onLongPress(product);
            return true;
        });

        holder.itemView.setOnClickListener(v -> {
            if (selectedProductIds.size() > 0) {
                if (selectedProductIds.contains(product.getId())) {
                    selectedProductIds.remove(product.getId());
                } else {
                    selectedProductIds.add(product.getId());
                }
                listener.onSelectionChanged(new HashSet<>(selectedProductIds));
                notifyDataSetChanged();
                return;
            }
            listener.onEdit(product);
        });
    }

    @Override
    public int getItemCount() {
        return products.size();
    }

    private String formatCurrency(double value) {
        NumberFormat format = NumberFormat.getCurrencyInstance(new Locale("pt", "BR"));
        return format.format(value);
    }

    private Bitmap getThumbnail(Uri uri) {
        String key = uri.toString();
        Bitmap cached = thumbnailCache.get(key);
        if (cached != null) {
            return cached;
        }
        try (InputStream boundsStream = context.getContentResolver().openInputStream(uri)) {
            if (boundsStream == null) {
                return null;
            }
            BitmapFactory.Options bounds = new BitmapFactory.Options();
            bounds.inJustDecodeBounds = true;
            BitmapFactory.decodeStream(boundsStream, null, bounds);
            int sample = 1;
            while (bounds.outWidth / sample > 256 || bounds.outHeight / sample > 256) {
                sample *= 2;
            }
            try (InputStream bitmapStream = context.getContentResolver().openInputStream(uri)) {
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inSampleSize = Math.max(1, sample);
                Bitmap bitmap = BitmapFactory.decodeStream(bitmapStream, null, options);
                if (bitmap != null) {
                    thumbnailCache.put(key, bitmap);
                }
                return bitmap;
            }
        } catch (Exception exception) {
            return null;
        }
    }

    static class ProductViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        TextView tvName;
        TextView tvDescription;
        TextView tvLocation;
        TextView tvStatus;
        TextView tvQuantity;
        TextView tvPrice;
        String boundImageKey;

        ProductViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
            tvName = itemView.findViewById(R.id.tvName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            tvStatus = itemView.findViewById(R.id.tvStatus);
            tvQuantity = itemView.findViewById(R.id.tvQuantity);
            tvPrice = itemView.findViewById(R.id.tvPrice);
        }
    }
}
