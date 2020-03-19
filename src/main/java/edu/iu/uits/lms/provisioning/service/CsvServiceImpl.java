package edu.iu.uits.lms.provisioning.service;

import com.opencsv.CSVWriter;
import lombok.extern.log4j.Log4j;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by chmaurer on 4/29/15.
 */
@Service
@Log4j
public class CsvServiceImpl implements CsvService {

    /**
     * Generic method to write csv files using the open CSVWriter. The headerArray can be null or empty and will still
     * function correctly. Will throw an exception if the stringArrayList is null or empty
     *
     * @param stringArrayList
     * @param headerArray
     * @param filePath
     * @return
     */
    @Override
    public void writeCsv(List<String[]> stringArrayList, String[] headerArray, String filePath) throws IOException {
        CSVWriter csvWriter = new CSVWriter(new FileWriter(filePath));
        if (headerArray!=null && headerArray.length>0) {
            csvWriter.writeNext(headerArray);
        }
        if (stringArrayList!=null && !stringArrayList.isEmpty()) {
            csvWriter.writeAll(stringArrayList);
        }
        csvWriter.flush();
        csvWriter.close();
    }

        /**
         * Use this to zip up csv files into one archive.  Provide a list of file names to archive and
         * the path/filename of the zip file to create
         *
         * @param fileList
         * @param zipFileName
         */
    @Override
    public void zipCsv(List<File> fileList, String zipFileName) {
        ZipOutputStream zos = null;
        try {
            log.info("Creating a zip file at " + zipFileName);
            zos = new ZipOutputStream(new FileOutputStream(zipFileName));
            for (File fileEntry : fileList) {
                addFileToZip(zos, fileEntry);
            }
        } catch (FileNotFoundException e) {
            log.error(e);
        } finally {
            if (zos != null) {
                try {
                    zos.finish();
                    zos.flush();
                    zos.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    private void addFileToZip(ZipOutputStream zos, File file) {
        FileInputStream fis = null;
        try {
            fis = new FileInputStream(file.getCanonicalFile());
            log.info("Adding " + file.getName() + " to the zip file");
            zos.putNextEntry(new ZipEntry(file.getName()));
            byte[] buf = new byte[1024];
            int len;
            while ((len = fis.read(buf)) > 0) {
                zos.write(buf, 0, len);
            }
            zos.closeEntry();
        } catch (FileNotFoundException e) {
            log.error(e);
        } catch (IOException e) {
            log.error(e);
        } finally {
            if (fis != null) {
                try {
                    fis.close();
                } catch (IOException e) {
                    log.error(e);
                }
            }
        }
    }

    /**
     * Class to filter files in a passed directory based on the file extension
     *
     * @param folder Folder containing the files to filter
     * @return List of .csv Files in the given folder
     */
    @Override
    public List<File> filterFiles(File folder) {
        List<File> approvedFileList = new ArrayList<File>();
        for (File fileEntry : folder.listFiles()) {
            if (fileEntry.isDirectory()) {
                filterFiles(fileEntry);
            } else {
                if (fileEntry.getPath().endsWith(".csv")) {
                    // Will want to make a version to check the headers, but assuming the file is correctly formatted for now
                    approvedFileList.add(fileEntry);
                }
            }
        }

        return approvedFileList;
    }
}
