package edu.udo.itmc.dev.blaesing.tsautils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.nio.file.Files;
import java.util.List;
import net.sourceforge.argparse4j.ArgumentParsers;
import net.sourceforge.argparse4j.impl.action.StoreTrueArgumentAction;
import net.sourceforge.argparse4j.inf.ArgumentParser;
import net.sourceforge.argparse4j.inf.ArgumentParserException;
import net.sourceforge.argparse4j.inf.Namespace;
import org.bouncycastle.tsp.TimeStampToken;

public class Main {
    public static void main(String[] args) throws Exception {
        ArgumentParser parser = ArgumentParsers.newFor("DFNTimestampService").build()
                .defaultHelp(true)
                .description("Create/verify TimeStampToken generated by the DFN TSA (https://www.pki.dfn.de/faqpki/faq-zeitstempel/)");
        parser.addArgument("--datafile")
                .nargs(1)
                .help("The contents to timestamp/verify")
                .type(File.class)
                .required(true);
        parser.addArgument("--tstfile")
                .nargs(1)
                .help("The input/output file for the timestamp")
                .type(File.class)
                .required(true);
        parser.addArgument("--verify")
                .help("Verify, that the supplied file matches the supplied timestamp file - on success error code is set to 0 and the timestamp is printed, on error error code is set to 1")
                .action(new StoreTrueArgumentAction());
        parser.addArgument("--timestamp")
                .help("Create a timestamp file for the supplied datafile. Error code is set to 0 on success and 1 on error.")
                .action(new StoreTrueArgumentAction());

        Namespace ns = null;
        try {
            ns = parser.parseArgs(args);
            if ((ns.getBoolean("verify") && ns.getBoolean("timestamp"))
                    || (ns.getBoolean("verify") == null) && (ns.getBoolean("timestamp") == null)) {
                throw new ArgumentParserException("Exactly one of --verify and --timestamp may be specified", parser);
            }
        } catch (ArgumentParserException e) {
            parser.handleError(e);
            System.exit(1);
        }

        File datafile = ((List<File>) ns.get("datafile")).get(0);
        File tstfile = ((List<File>) ns.get("tstfile")).get(0);

        DFNTimestampService ts = new DFNTimestampService();

        if(ns.getBoolean("verify")) {
            try(FileInputStream data = new FileInputStream(datafile)) {
                byte[] timestampData = Files.readAllBytes(tstfile.toPath());
                System.out.println("Valid timestamp for: " + ts.validate(data, timestampData));
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                System.exit(1);
            }
        } else if (ns.getBoolean("timestamp")) {
            try(FileInputStream data = new FileInputStream(datafile);
                    FileOutputStream timestamp = new FileOutputStream(tstfile)) {
                TimeStampToken token = ts.timestamp(data);
                timestamp.write(token.getEncoded());
            } catch (Exception ex) {
                ex.printStackTrace(System.err);
                System.exit(1);
            }
        }
    }

}
