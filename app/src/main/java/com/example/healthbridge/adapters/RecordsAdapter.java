package com.example.healthbridge.adapters;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.healthbridge.R;
import com.example.healthbridge.models.MedicalRecord;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * RecyclerView adapter for displaying medical documents in the Vault.
 */
public class RecordsAdapter extends RecyclerView.Adapter<RecordsAdapter.RecordViewHolder> {

    public interface OnRecordClickListener {
        void onRecordClick(MedicalRecord record);
    }

    private final List<MedicalRecord> allRecords = new ArrayList<>();
    private final List<MedicalRecord> displayedRecords = new ArrayList<>();
    private OnRecordClickListener listener;

    public RecordsAdapter() {
    }

    public RecordsAdapter(OnRecordClickListener listener) {
        this.listener = listener;
    }

    public void setOnRecordClickListener(OnRecordClickListener listener) {
        this.listener = listener;
    }

    /**
     * Sets the complete record set and reapplies the current filter.
     */
    public void setRecords(List<MedicalRecord> records) {
        allRecords.clear();
        displayedRecords.clear();
        if (records != null) {
            allRecords.addAll(records);
            displayedRecords.addAll(records);
        }
        notifyDataSetChanged();
    }

    /**
     * Filters documents by query (case-insensitive on title) and selected category.
     * Trims query and treats null titles as empty string.
     */
    public void applyFilter(String query, String category) {
        displayedRecords.clear();
        String trimmedQuery = (query != null) ? query.trim().toLowerCase(Locale.ROOT) : "";
        boolean filterCategory = (category != null && !category.equalsIgnoreCase("All"));

        for (MedicalRecord record : allRecords) {
            if (record == null) continue;

            // Category filter
            if (filterCategory) {
                String recordCat = record.getCategory();
                if (!matchesCategory(recordCat, category)) {
                    continue;
                }
            }

            // Title filter
            if (!trimmedQuery.isEmpty()) {
                String title = record.getTitle();
                String titleLower = (title != null) ? title.trim().toLowerCase(Locale.ROOT) : "";
                if (!titleLower.contains(trimmedQuery)) {
                    continue;
                }
            }

            displayedRecords.add(record);
        }
        notifyDataSetChanged();
    }

    public int getDisplayedCount() {
        return displayedRecords.size();
    }

    public int getTotalCount() {
        return allRecords.size();
    }

    @NonNull
    @Override
    public RecordViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_record, parent, false);
        return new RecordViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecordViewHolder holder, int position) {
        MedicalRecord record = displayedRecords.get(position);
        Context context = holder.itemView.getContext();

        if (record == null) {
            holder.tvTitle.setText("Untitled document");
            holder.tvDate.setText("Not provided");
            holder.tvCategory.setText("General");
            return;
        }

        // Title: fallback if empty or null
        String title = record.getTitle();
        if (title == null || title.trim().isEmpty()) {
            holder.tvTitle.setText("Untitled document");
        } else {
            holder.tvTitle.setText(title.trim());
        }

        // Date: format created_at as "dd MMM yyyy"
        holder.tvDate.setText(formatDisplayDate(record.getCreatedAt()));

        // Category & Tag Colors
        String category = record.getCategory();
        applyCategoryTag(holder.tvCategory, category, context);

        // Icon Logic: PDF vs Image / Category
        String mimeType = record.getMimeType();
        applyIconLogic(holder.ivIconTile, holder.ivIcon, mimeType, category, context);

        // Click Handling
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onRecordClick(record);
            }
        });
    }

    @Override
    public int getItemCount() {
        return displayedRecords.size();
    }

    private void applyIconLogic(FrameLayout ivIconTile, ImageView ivIcon, String mimeType, String category, Context context) {
        boolean isPdf = mimeType != null && mimeType.equalsIgnoreCase("application/pdf");
        boolean isImage = mimeType != null && mimeType.startsWith("image/");

        int tileBgColor;
        int iconColor;

        if (isPdf) {
            tileBgColor = ContextCompat.getColor(context, R.color.hb_cat_pdf_bg);
            iconColor = ContextCompat.getColor(context, R.color.hb_cat_pdf_text);
        } else if (isImage) {
            int[] categoryColors = getCategoryColors(category, context);
            tileBgColor = categoryColors[0];
            iconColor = categoryColors[1];
        } else {
            int[] categoryColors = getCategoryColors(category, context);
            tileBgColor = categoryColors[0];
            iconColor = categoryColors[1];
        }

        ivIconTile.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_icon_tile));
        ivIconTile.setBackgroundTintList(ColorStateList.valueOf(tileBgColor));
        ivIcon.setColorFilter(iconColor);
    }

    private void applyCategoryTag(TextView tvCategory, String category, Context context) {
        String displayText;
        if (category == null || category.trim().isEmpty()) {
            displayText = "General";
        } else {
            displayText = category.trim();
        }

        int[] colors = getCategoryColors(displayText, context);
        tvCategory.setText(displayText);
        tvCategory.setBackground(ContextCompat.getDrawable(context, R.drawable.bg_tag));
        tvCategory.setBackgroundTintList(ColorStateList.valueOf(colors[0]));
        tvCategory.setTextColor(colors[1]);
    }

    private int[] getCategoryColors(String category, Context context) {
        int bg;
        int text;
        String cat = (category != null) ? category.trim() : "";

        if (cat.equalsIgnoreCase("Prescriptions") || cat.equalsIgnoreCase("Prescription")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_prescription_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_prescription_text);
        } else if (cat.equalsIgnoreCase("Lab Reports") || cat.equalsIgnoreCase("Lab Report")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_lab_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_lab_text);
        } else if (cat.equalsIgnoreCase("X-Rays") || cat.equalsIgnoreCase("X-Ray")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_xray_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_xray_text);
        } else if (cat.equalsIgnoreCase("Medical History")) {
            bg = ContextCompat.getColor(context, R.color.hb_cat_history_bg);
            text = ContextCompat.getColor(context, R.color.hb_cat_history_text);
        } else {
            bg = ContextCompat.getColor(context, R.color.hb_glass_fill);
            text = ContextCompat.getColor(context, R.color.hb_text_secondary);
        }
        return new int[]{bg, text};
    }

    private boolean matchesCategory(String recordCategory, String filterCategory) {
        if (recordCategory == null) return false;
        String r = recordCategory.trim();
        String f = filterCategory.trim();
        if (r.equalsIgnoreCase(f)) return true;
        if (r.endsWith("s") && r.substring(0, r.length() - 1).equalsIgnoreCase(f)) return true;
        if (f.endsWith("s") && f.substring(0, f.length() - 1).equalsIgnoreCase(r)) return true;
        return false;
    }

    private String formatDisplayDate(String rawDate) {
        if (rawDate == null || rawDate.trim().isEmpty()) {
            return "Not provided";
        }
        try {
            String trimmed = rawDate.trim();
            SimpleDateFormat outFormat = new SimpleDateFormat("dd MMM yyyy", Locale.getDefault());
            if (trimmed.contains("T")) {
                String datePart = trimmed.split("T")[0];
                SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsed = inFormat.parse(datePart);
                if (parsed != null) {
                    return outFormat.format(parsed);
                }
            } else if (trimmed.length() >= 10) {
                SimpleDateFormat inFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                Date parsed = inFormat.parse(trimmed.substring(0, 10));
                if (parsed != null) {
                    return outFormat.format(parsed);
                }
            }
        } catch (Exception ignored) {
        }
        return "Not provided";
    }

    static class RecordViewHolder extends RecyclerView.ViewHolder {
        final FrameLayout ivIconTile;
        final ImageView ivIcon;
        final TextView tvTitle;
        final TextView tvDate;
        final TextView tvCategory;
        final ImageView ivChevron;

        public RecordViewHolder(@NonNull View itemView) {
            super(itemView);
            ivIconTile = itemView.findViewById(R.id.ivIconTile);
            ivIcon = itemView.findViewById(R.id.ivIcon);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvCategory = itemView.findViewById(R.id.tvCategory);
            ivChevron = itemView.findViewById(R.id.ivChevron);
        }
    }
}
