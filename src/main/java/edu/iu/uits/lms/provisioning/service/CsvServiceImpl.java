package edu.iu.uits.lms.provisioning.service;

/*-
 * #%L
 * lms-lti-3rdpartyprovisioning
 * %%
 * Copyright (C) 2015 - 2022 Indiana University
 * %%
 * Redistribution and use in source and binary forms, with or without modification,
 * are permitted provided that the following conditions are met:
 * 
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 * 
 * 3. Neither the name of the Indiana University nor the names of its contributors
 *    may be used to endorse or promote products derived from this software without
 *    specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED.
 * IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING,
 * BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF
 * LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED
 * OF THE POSSIBILITY OF SUCH DAMAGE.
 * #L%
 */

import com.opencsv.CSVWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Created by chmaurer on 4/29/15.
 */
@Service
@Slf4j
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

    @Override
    public InputStream writeCsvToStream(List<String[]> stringArrayList, String[] headerArray) throws IOException {
        byte[] bytes = writeCsvToBytes(stringArrayList, headerArray);

        //use ByteArrayInputStream to get the bytes of the String and convert them to InputStream.
        InputStream inputStream = new ByteArrayInputStream(bytes);
        return inputStream;
    }

    @Override
    public byte[] writeCsvToBytes(List<String[]> stringArrayList, String[] headerArray) throws IOException {
        StringWriter stringWriter = new StringWriter();
        CSVWriter csvWriter = new CSVWriter(stringWriter);
        if (headerArray!=null && headerArray.length>0) {
            csvWriter.writeNext(headerArray);
        }
        if (stringArrayList!=null && !stringArrayList.isEmpty()) {
            csvWriter.writeAll(stringArrayList);
        }
        csvWriter.flush();
        csvWriter.close();

        //use ByteArrayInputStream to get the bytes of the String and convert them to InputStream.
        return stringWriter.toString().getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Use this to zip up csv files into one archive.  Provide a list of file names to archive and
     * the path/filename of the zip file to create
     *
     * @param fileList
     * @param filePath
     */
    @Override
    public File zipCsv(List<ProvisioningResult.FileObject> fileList, String filePath) {
        File file = new File(filePath);
        ZipOutputStream zos = null;
        try {
            zos = new ZipOutputStream(new FileOutputStream(file));
            for (ProvisioningResult.FileObject fileEntry : fileList) {
                addFileToZip(zos, fileEntry);
            }
        } catch (FileNotFoundException e) {
            log.error("file not found", e);
        } finally {
            if (zos != null) {
                try {
                    zos.finish();
                    zos.flush();
                    zos.close();
                } catch (IOException e) {
                    log.error("cannot cloes up zip stream", e);
                }
            }
        }

        return file;
    }

    private void addFileToZip(ZipOutputStream zos, ProvisioningResult.FileObject fileObject) {
        byte[] fileBytes = fileObject.getFileBytes();
        if (fileBytes != null) {
            try {
                log.info("Adding " + fileObject.getFileName() + " to the zip file");
                zos.putNextEntry(new ZipEntry(fileObject.getFileName()));
                zos.write(fileBytes);
                zos.closeEntry();
            } catch (IOException e) {
                log.error("error zipping file", e);
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
