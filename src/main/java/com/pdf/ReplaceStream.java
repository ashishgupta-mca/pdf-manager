package com.pdf;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

import com.itextpdf.text.DocumentException;
import com.itextpdf.text.pdf.PRStream;
import com.itextpdf.text.pdf.PdfDictionary;
import com.itextpdf.text.pdf.PdfName;
import com.itextpdf.text.pdf.PdfObject;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfStamper;
import com.pdf.config.Configuration;

public class ReplaceStream {

	private Configuration configuration = new Configuration();

	public void manipulatePdf(String dest, Map<String, List<String>> params) throws IOException, DocumentException {
		PdfReader reader = new PdfReader(configuration.readPath());
		for (int i = 1; i <= reader.getNumberOfPages(); i++) {
			PdfDictionary dict = reader.getPageN(i);
			PdfObject object = dict.getDirectObject(PdfName.CONTENTS);
			if (object instanceof PRStream) {
				PRStream stream = (PRStream) object;
				for (String key : params.keySet()) {
					byte[] data = PdfReader.getStreamBytes(stream);
					stream.setData(new String(data).replace(key, params.get(key).get(0)).getBytes());
				}
			}
		}

		PdfStamper stamper = new PdfStamper(reader, new FileOutputStream(configuration.getGeneratedPath(dest)));
		stamper.close();
		reader.close();
	}

	public Configuration getConfiguration() {
		return configuration;
	}

}