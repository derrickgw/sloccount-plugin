package hudson.plugins.sloccount.model;

import hudson.FilePath;
import hudson.plugins.sloccount.util.FileFinder;
import hudson.remoting.VirtualChannel;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.Reader;

/**
 * 
 * @author lordofthepigs
 */
public class SloccountParser implements
        FilePath.FileCallable<SloccountPublisherReport> {

    private static final boolean LOG_ENABLED = false;

    private final String encoding;
    private final String filePattern;
    private transient PrintStream logger = null;

    public SloccountParser(String encoding, String filePattern, PrintStream logger){
        this.logger = logger;
        this.filePattern = filePattern;
        this.encoding = encoding;
    }


    public SloccountPublisherReport invoke(java.io.File workspace, VirtualChannel channel) throws IOException {
        SloccountPublisherReport report = new SloccountPublisherReport();

        FileFinder finder = new FileFinder(this.filePattern);
        String[] found = finder.find(workspace);

        for(String fileName : found){
            this.parse(new java.io.File(workspace, fileName), report);
            report.addSourceFile(new java.io.File(workspace, fileName));
        }

        report.simplifyNames();
        return report;
    }
    
    /**
     * Parse a list of input files. All errors are silently ignored.
     * 
     * @param files
     *            the files
     * @return the content of the parsed files in form of a report
     */
    public SloccountReport parseFiles(java.io.File[] files) {
        SloccountReport report = new SloccountReport();

        for (java.io.File file : files) {
            try {
                parse(file, report);
            } catch (IOException e) {
                // Silently ignore, there is still a possibility that other
                // files can be parsed successfully
            }
        }

        return report;
    }

    private void parse(java.io.File file, SloccountReportInterface report) throws IOException {
        InputStreamReader in = null;
        
        try {
            in = new InputStreamReader(new FileInputStream(file), encoding);
            this.parse(in, report);
        } finally {
            in.close();
        }
    }

    private void parse(Reader reader, SloccountReportInterface report) throws IOException {
        BufferedReader in = new BufferedReader(reader);

        String line;
        while((line = in.readLine()) != null){
            this.parseLine(line, report);
        }

        if(LOG_ENABLED && (this.logger != null)){
            this.logger.println("Root folder is: " + report.getRootFolder());
        }
    }

    private void parseLine(String line, SloccountReportInterface report){
        String[] tokens = line.split("\t");

        if(tokens.length != 4){
            // line is not a line count report line, ignore
            if(LOG_ENABLED && (this.logger != null)){
                logger.println("Ignoring line: " + line);
            }
            return;
        }

        if(LOG_ENABLED && (this.logger != null)){
            logger.println("Parsing line: " + line);
        }

        int lineCount = Integer.parseInt(tokens[0]);
        String languageName = tokens[1];
        String filePath = tokens[3];

        if(LOG_ENABLED && (this.logger != null)){
            logger.println("lineCount: " + lineCount);
            logger.println("language : " + languageName);
            logger.println("file : " + filePath);
        }

        report.add(filePath, languageName, lineCount);
    }
}