package net.ekstrandom.pdftools;

import com.itextpdf.kernel.pdf.*;
import com.itextpdf.kernel.utils.PdfMerger;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.Arguments;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.Namespace;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.List;
import java.util.Optional;
import java.util.StringJoiner;

/**
 * Concatenate PDF files.
 */
public class CatPDF {
    private static final Logger logger = LoggerFactory.getLogger(CatPDF.class);
    private final Namespace options;

    public static void main(String[] args) {
        ArgumentParser parser = makeArgumentParser();
        Namespace options = parser.parseArgsOrFail(args);
        CatPDF cat = new CatPDF(options);
        cat.run();
    }

    public CatPDF(Namespace opts) {
        this.options = opts;
    }

    public File getOutputFile() {
        return options.get("output_file");
    }

    public Optional<String> getEncryptPassword() {
        return Optional.ofNullable(options.getString("encrypt_password"));
    }

    public List<File> getInputFiles() {
        return options.getList("input_files");
    }

    public boolean includeMetadata() {
        return options.getBoolean("metadata");
    }

    public void run() {
        logger.info("preparing to write to {}", getOutputFile());
        WriterProperties wprops = new WriterProperties();
        Optional<String> pw = getEncryptPassword();
        if (pw.isPresent()) {
            logger.info("encrypting output PDF");
            byte[] pwb = pw.get().getBytes();
            wprops.setStandardEncryption(pwb, pwb, 0,
                                         EncryptionConstants.ENCRYPTION_AES_128);
        }
        try (OutputStream out = new FileOutputStream(getOutputFile());
             PdfWriter writer = new PdfWriter(out, wprops);
             PdfDocument result = new PdfDocument(writer)) {

            PdfMerger merger = new PdfMerger(result);
            boolean mdDone = false;

            for (File inFile: getInputFiles()) {
                logger.debug("reading {}", inFile);
                try (PdfReader reader = new PdfReader(inFile.getPath());
                     PdfDocument src = new PdfDocument(reader)) {
                    if (src.hasOutlines() && !result.hasOutlines()) {
                        logger.debug("initializing outlines");
                        result.initializeOutlines();
                    }
                    if (!mdDone && includeMetadata()) {
                        logger.info("adding document info from {}", inFile);
                        PdfDocumentInfo info = src.getDocumentInfo();
                        PdfDocumentInfo oi = result.getDocumentInfo();
                        String field = info.getTitle();
                        if (field != null) {
                            oi.setTitle(field);
                        }
                        field = info.getCreator();
                        if (field != null) {
                            oi.setCreator(field);
                        }
                        field = info.getAuthor();
                        if (field != null) {
                            oi.setAuthor(field);
                        }
                        field = info.getKeywords();
                        if (field != null) {
                            oi.setKeywords(field);
                        }
                        field = info.getSubject();
                        if (field != null) {
                            oi.setSubject(field);
                        }
                        mdDone = true;
                    }
                    logger.info("adding pages from {}", inFile);
                    merger.merge(src, 1, src.getNumberOfPages());
                }
            }

            merger.close();
        } catch (IOException ex) {
            throw new UncheckedIOException(ex);
        }
    }

    /**
     * Make the argument parser for this program.
     * @return The argument parser.
     */
    private static ArgumentParser makeArgumentParser() {
        ArgumentParser parser = ArgumentParsers.newArgumentParser("pdf-cat");
        parser.addArgument("-o", "--output-file")
              .type(File.class)
              .metavar("FILE")
              .setDefault(new File("output.pdf"))
              .help("write output to FILE");
        parser.addArgument("--encrypt-password", "--user-password")
              .metavar("PASS")
              .help("encrypt with password PASS");
        parser.addArgument("--no-metadata")
              .dest("metadata")
              .action(Arguments.storeFalse())
              .help("Do not include metadata");
        parser.addArgument("input_files")
              .type(File.class)
              .nargs("+")
              .help("read from specified files");
        return parser;
    }
}
