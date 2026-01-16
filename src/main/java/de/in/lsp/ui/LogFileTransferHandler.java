package de.in.lsp.ui;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.io.File;
import java.util.List;
import java.util.function.Consumer;

import javax.swing.TransferHandler;

/**
 * Handles Drag & Drop file imports for LogSyncPro.
 * 
 * @author TiJaWo68 in cooperation with Gemini 3 Flash using Antigravity
 */
public class LogFileTransferHandler extends TransferHandler {

    private final Consumer<List<File>> onFilesDropped;

    public LogFileTransferHandler(Consumer<List<File>> onFilesDropped) {
        this.onFilesDropped = onFilesDropped;
    }

    @Override
    public boolean canImport(TransferSupport support) {
        return support.isDataFlavorSupported(DataFlavor.javaFileListFlavor);
    }

    @Override
    public boolean importData(TransferSupport support) {
        if (!canImport(support))
            return false;
        try {
            Transferable t = support.getTransferable();
            @SuppressWarnings("unchecked")
            List<File> files = (List<File>) t.getTransferData(DataFlavor.javaFileListFlavor);
            if (files != null && !files.isEmpty()) {
                onFilesDropped.accept(files);
                return true;
            }
        } catch (Exception e) {
            // Ignore (log if possible, but here we just return false)
        }
        return false;
    }
}
