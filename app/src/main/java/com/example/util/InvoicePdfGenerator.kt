package com.example.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.example.model.Invoice
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object InvoicePdfGenerator {

    // Helper to draw text with wrapping and alignment using StaticLayout
    private fun drawTextLayout(
        canvas: Canvas,
        text: String,
        paint: TextPaint,
        width: Int,
        x: Float,
        y: Float,
        align: Layout.Alignment = Layout.Alignment.ALIGN_NORMAL
    ): Int {
        canvas.save()
        canvas.translate(x, y)
        val staticLayout = StaticLayout(
            text,
            paint,
            width,
            align,
            1.0f,
            0.0f,
            false
        )
        staticLayout.draw(canvas)
        canvas.restore()
        return staticLayout.height
    }

    fun generateA4Pdf(
        context: Context,
        invoice: Invoice,
        shopName: String,
        ownerPhone: String?
    ): File {
        val pdfDocument = PdfDocument()
        val pageInfo = PdfDocument.PageInfo.Builder(595, 842, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paint definitions
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 10f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerPaint = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 22f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val subtitlePaint = TextPaint().apply {
            color = Color.DKGRAY
            isAntiAlias = true
            textSize = 9f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val linePaint = Paint().apply {
            color = Color.LTGRAY
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val thickLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1.5f
            style = Paint.Style.STROKE
        }

        // Margins and positions
        val leftMargin = 40f
        val rightMargin = 555f
        val contentWidth = (rightMargin - leftMargin).toInt()
        var currentY = 40f

        // 1. Draw Header
        val headerHeight = drawTextLayout(
            canvas,
            shopName.uppercase(Locale.getDefault()),
            headerPaint,
            contentWidth,
            leftMargin,
            currentY,
            Layout.Alignment.ALIGN_NORMAL
        )
        currentY += headerHeight + 4f

        val taglineHeight = drawTextLayout(
            canvas,
            "Smart Hisab Digital Invoice & Inventory Manager",
            subtitlePaint,
            contentWidth,
            leftMargin,
            currentY,
            Layout.Alignment.ALIGN_NORMAL
        )
        currentY += taglineHeight + 12f

        // Draw basic contact / phone if available
        if (!ownerPhone.isNullOrBlank()) {
            val phoneHeight = drawTextLayout(
                canvas,
                "Phone: $ownerPhone",
                textPaint,
                contentWidth,
                leftMargin,
                currentY,
                Layout.Alignment.ALIGN_NORMAL
            )
            currentY += phoneHeight + 6f
        }

        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, thickLinePaint)
        currentY += 15f

        // 2. Invoice Meta (Invoice Number, Date, Customer Details)
        val formattedDate = try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
            sdf.format(Date(invoice.createdAt))
        } catch (e: Exception) {
            "Just Now"
        }

        // Draw Metadata Left/Right
        val metaY = currentY
        val leftColWidth = contentWidth / 2
        
        val invNoHeight = drawTextLayout(
            canvas,
            "INVOICE NO: ${invoice.invoiceNumber}",
            boldPaint,
            leftColWidth,
            leftMargin,
            metaY,
            Layout.Alignment.ALIGN_NORMAL
        )
        
        drawTextLayout(
            canvas,
            "DATE: $formattedDate",
            textPaint,
            leftColWidth,
            leftMargin + leftColWidth,
            metaY,
            Layout.Alignment.ALIGN_OPPOSITE
        )
        
        currentY += maxOf(invNoHeight, 14) + 6f

        // Customer details
        invoice.customerName?.let { name ->
            val custHeight = drawTextLayout(
                canvas,
                "BILL TO: $name",
                boldPaint,
                contentWidth,
                leftMargin,
                currentY,
                Layout.Alignment.ALIGN_NORMAL
            )
            currentY += custHeight + 10f
        } ?: run {
            currentY += 10f
        }

        // 3. Draw Items Table Header
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, thickLinePaint)
        currentY += 6f

        val colItemWidth = (contentWidth * 0.5).toInt()
        val colPriceWidth = (contentWidth * 0.15).toInt()
        val colQtyWidth = (contentWidth * 0.15).toInt()
        val colTotalWidth = (contentWidth * 0.2).toInt()

        drawTextLayout(canvas, "ITEM DESCRIPTION", boldPaint, colItemWidth, leftMargin, currentY, Layout.Alignment.ALIGN_NORMAL)
        drawTextLayout(canvas, "PRICE", boldPaint, colPriceWidth, leftMargin + colItemWidth, currentY, Layout.Alignment.ALIGN_OPPOSITE)
        drawTextLayout(canvas, "QTY", boldPaint, colQtyWidth, leftMargin + colItemWidth + colPriceWidth, currentY, Layout.Alignment.ALIGN_OPPOSITE)
        drawTextLayout(canvas, "TOTAL", boldPaint, colTotalWidth, leftMargin + colItemWidth + colPriceWidth + colQtyWidth, currentY, Layout.Alignment.ALIGN_OPPOSITE)

        currentY += 16f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, linePaint)
        currentY += 8f

        // 4. Draw Table Items
        for (item in invoice.items) {
            val startY = currentY
            
            // Draw item name (supports dynamic wrapping)
            val nameHeight = drawTextLayout(
                canvas,
                item.productName,
                textPaint,
                colItemWidth,
                leftMargin,
                startY,
                Layout.Alignment.ALIGN_NORMAL
            )

            // Draw other cols
            drawTextLayout(
                canvas,
                "Rs ${item.salePrice.toInt()}",
                textPaint,
                colPriceWidth,
                leftMargin + colItemWidth,
                startY,
                Layout.Alignment.ALIGN_OPPOSITE
            )

            drawTextLayout(
                canvas,
                item.quantity.toString(),
                textPaint,
                colQtyWidth,
                leftMargin + colItemWidth + colPriceWidth,
                startY,
                Layout.Alignment.ALIGN_OPPOSITE
            )

            drawTextLayout(
                canvas,
                "Rs ${item.lineTotal.toInt()}",
                boldPaint,
                colTotalWidth,
                leftMargin + colItemWidth + colPriceWidth + colQtyWidth,
                startY,
                Layout.Alignment.ALIGN_OPPOSITE
            )

            currentY += maxOf(nameHeight, 14) + 6f
            
            // Draw divider
            canvas.drawLine(leftMargin, currentY, rightMargin, currentY, linePaint)
            currentY += 8f
        }

        // 5. Draw Summary Calculations Block
        currentY += 10f
        val summaryLeft = leftMargin + (contentWidth * 0.4).toFloat()
        val summaryWidth = (contentWidth * 0.6).toInt()

        val summaries = mutableListOf<Pair<String, String>>()
        summaries.add("Subtotal:" to "Rs ${invoice.subtotal.toInt()}")
        if (invoice.discountAmount > 0.0) {
            summaries.add("Discount:" to "- Rs ${invoice.discountAmount.toInt()}")
        }
        invoice.taxAmount?.let { tax ->
            if (tax > 0.0) {
                summaries.add("Tax:" to "Rs ${tax.toInt()}")
            }
        }
        summaries.add("GRAND TOTAL:" to "Rs ${invoice.totalAmount.toInt()}")
        summaries.add("Paid Amount:" to "Rs ${invoice.paidAmount.toInt()}")
        summaries.add("Remaining Balance:" to "Rs ${invoice.remainingAmount.toInt()}")
        summaries.add("Payment Status:" to invoice.paymentStatus.uppercase(Locale.getDefault()))

        for ((label, valStr) in summaries) {
            val isTotal = label.startsWith("GRAND TOTAL")
            val paintToUse = if (isTotal) boldPaint else textPaint
            
            val labelHeight = drawTextLayout(
                canvas,
                label,
                paintToUse,
                summaryWidth / 2,
                summaryLeft,
                currentY,
                Layout.Alignment.ALIGN_NORMAL
            )
            
            drawTextLayout(
                canvas,
                valStr,
                paintToUse,
                summaryWidth / 2,
                summaryLeft + (summaryWidth / 2),
                currentY,
                Layout.Alignment.ALIGN_OPPOSITE
            )
            
            currentY += labelHeight + 4f
            if (isTotal) {
                canvas.drawLine(summaryLeft, currentY, rightMargin, currentY, linePaint)
                currentY += 6f
            }
        }

        // Footer Thank You Message
        val footerY = 780f
        canvas.drawLine(leftMargin, footerY - 10f, rightMargin, footerY - 10f, thickLinePaint)
        drawTextLayout(
            canvas,
            "Thank you for your business!\nPowered by Smart Hisab",
            subtitlePaint,
            contentWidth,
            leftMargin,
            footerY,
            Layout.Alignment.ALIGN_CENTER
        )

        pdfDocument.finishPage(page)

        // Save PDF to cached temporary file
        val file = File(context.cacheDir, "SmartHisab_Invoice_${invoice.invoiceNumber}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()
        
        return file
    }

    fun generateThermalPdf(
        context: Context,
        invoice: Invoice,
        shopName: String,
        ownerPhone: String?
    ): File {
        val pdfDocument = PdfDocument()

        // Page Specs for 80mm/3inch thermal paper representation (300 points width)
        val pageWidth = 300
        
        // Compute dynamic height based on item list size
        val itemLineHeight = 24
        val headerHeight = 150
        val dividerLinesHeight = 50
        val itemsHeight = invoice.items.size * itemLineHeight
        val summaryHeight = 160
        val footerHeight = 60
        val pageHeight = headerHeight + dividerLinesHeight + itemsHeight + summaryHeight + footerHeight

        val pageInfo = PdfDocument.PageInfo.Builder(pageWidth, pageHeight, 1).create()
        val page = pdfDocument.startPage(pageInfo)
        val canvas = page.canvas

        // Paint definitions
        val textPaint = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.NORMAL)
        }

        val boldPaint = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 8.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val headerPaint = TextPaint().apply {
            color = Color.BLACK
            isAntiAlias = true
            textSize = 14f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
        }

        val footerPaint = TextPaint().apply {
            color = Color.DKGRAY
            isAntiAlias = true
            textSize = 7.5f
            typeface = Typeface.create(Typeface.DEFAULT, Typeface.ITALIC)
        }

        val dottedLinePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 1f
            style = Paint.Style.STROKE
        }

        val leftMargin = 15f
        val rightMargin = 285f
        val contentWidth = rightMargin - leftMargin
        var currentY = 15f

        // 1. Draw Centered Header
        val titleHeight = drawTextLayout(
            canvas,
            shopName.uppercase(Locale.getDefault()),
            headerPaint,
            contentWidth.toInt(),
            leftMargin,
            currentY,
            Layout.Alignment.ALIGN_CENTER
        )
        currentY += titleHeight + 3f

        drawTextLayout(
            canvas,
            "SMART HISAB DIGITAL RECEIPT",
            textPaint,
            contentWidth.toInt(),
            leftMargin,
            currentY,
            Layout.Alignment.ALIGN_CENTER
        )
        currentY += 12f

        if (!ownerPhone.isNullOrBlank()) {
            drawTextLayout(
                canvas,
                "Contact: $ownerPhone",
                textPaint,
                contentWidth.toInt(),
                leftMargin,
                currentY,
                Layout.Alignment.ALIGN_CENTER
            )
            currentY += 12f
        }

        // Dotted divider
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, dottedLinePaint)
        currentY += 8f

        // 2. Receipt metadata
        val formattedDate = try {
            val sdf = SimpleDateFormat("dd/MM/yy hh:mm a", Locale.getDefault())
            sdf.format(Date(invoice.createdAt))
        } catch (e: Exception) {
            "Now"
        }

        drawTextLayout(canvas, "INV: ${invoice.invoiceNumber}", boldPaint, (contentWidth/2).toInt(), leftMargin, currentY, Layout.Alignment.ALIGN_NORMAL)
        drawTextLayout(canvas, formattedDate, textPaint, (contentWidth/2).toInt(), leftMargin + (contentWidth/2), currentY, Layout.Alignment.ALIGN_OPPOSITE)
        currentY += 12f

        invoice.customerName?.let { name ->
            drawTextLayout(canvas, "CUST: $name", textPaint, contentWidth.toInt(), leftMargin, currentY, Layout.Alignment.ALIGN_NORMAL)
            currentY += 12f
        }

        // Dotted divider
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, dottedLinePaint)
        currentY += 8f

        // 3. Table Column Headers
        val colItemW = (contentWidth * 0.55).toInt()
        val colQtyW = (contentWidth * 0.15).toInt()
        val colTotalW = (contentWidth * 0.3).toInt()

        drawTextLayout(canvas, "ITEM", boldPaint, colItemW, leftMargin, currentY, Layout.Alignment.ALIGN_NORMAL)
        drawTextLayout(canvas, "QTY", boldPaint, colQtyW, leftMargin + colItemW, currentY, Layout.Alignment.ALIGN_OPPOSITE)
        drawTextLayout(canvas, "TOTAL", boldPaint, colTotalW, leftMargin + colItemW + colQtyW, currentY, Layout.Alignment.ALIGN_OPPOSITE)
        
        currentY += 12f
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, dottedLinePaint)
        currentY += 8f

        // 4. Products List
        for (item in invoice.items) {
            val itemStartY = currentY
            
            val itemH = drawTextLayout(
                canvas,
                "${item.productName}\n  @ Rs ${item.salePrice.toInt()}",
                textPaint,
                colItemW,
                leftMargin,
                itemStartY,
                Layout.Alignment.ALIGN_NORMAL
            )

            drawTextLayout(
                canvas,
                item.quantity.toString(),
                textPaint,
                colQtyW,
                leftMargin + colItemW,
                itemStartY,
                Layout.Alignment.ALIGN_OPPOSITE
            )

            drawTextLayout(
                canvas,
                "Rs ${item.lineTotal.toInt()}",
                boldPaint,
                colTotalW,
                leftMargin + colItemW + colQtyW,
                itemStartY,
                Layout.Alignment.ALIGN_OPPOSITE
            )

            currentY += maxOf(itemH, 14) + 4f
        }

        // Dotted divider
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, dottedLinePaint)
        currentY += 8f

        // 5. Summary calculation
        val summaryW = contentWidth.toInt()
        val summaries = mutableListOf<Pair<String, String>>()
        summaries.add("Subtotal:" to "Rs ${invoice.subtotal.toInt()}")
        if (invoice.discountAmount > 0.0) {
            summaries.add("Discount:" to "- Rs ${invoice.discountAmount.toInt()}")
        }
        invoice.taxAmount?.let { tax ->
            if (tax > 0.0) {
                summaries.add("Tax:" to "Rs ${tax.toInt()}")
            }
        }
        summaries.add("GRAND TOTAL:" to "Rs ${invoice.totalAmount.toInt()}")
        summaries.add("Paid:" to "Rs ${invoice.paidAmount.toInt()}")
        summaries.add("Remaining:" to "Rs ${invoice.remainingAmount.toInt()}")
        summaries.add("Status:" to invoice.paymentStatus.uppercase(Locale.getDefault()))

        for ((label, valStr) in summaries) {
            val isTotal = label.startsWith("GRAND TOTAL")
            val pUse = if (isTotal) boldPaint else textPaint

            drawTextLayout(canvas, label, pUse, summaryW / 2, leftMargin, currentY, Layout.Alignment.ALIGN_NORMAL)
            drawTextLayout(canvas, valStr, pUse, summaryW / 2, leftMargin + (summaryW / 2), currentY, Layout.Alignment.ALIGN_OPPOSITE)
            currentY += 12f
        }

        // Dotted divider
        canvas.drawLine(leftMargin, currentY, rightMargin, currentY, dottedLinePaint)
        currentY += 8f

        // 6. Centered footer
        drawTextLayout(
            canvas,
            "Thank You For Shopping!\nSmart Hisab Digital System",
            footerPaint,
            contentWidth.toInt(),
            leftMargin,
            currentY,
            Layout.Alignment.ALIGN_CENTER
        )

        pdfDocument.finishPage(page)

        // Save thermal PDF to cached temporary file
        val file = File(context.cacheDir, "SmartHisab_Thermal_${invoice.invoiceNumber}.pdf")
        pdfDocument.writeTo(FileOutputStream(file))
        pdfDocument.close()

        return file
    }
}
