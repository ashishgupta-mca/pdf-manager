package com.pdf.config;

import java.io.File;
import java.io.IOException;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;

public class Configuration {

	String PATH = "conf/worthroom.pdf";

	String GENERATED_PATH = "generated";

	// for linux

	String L_PATH = "/etc/worthroom-suite/conf/worthroom.pdf";

	String L_GENERATED_PATH = "generated";

	public String readPath() throws IOException {
		Resource resource = new FileSystemResource(getPath());
		return resource.getFile().getAbsolutePath();
	}

	public String getGeneratedPath(String file) throws IOException {
		Resource resource = new FileSystemResource(getPath());
		return new FileSystemResource(
				resource.getFile().getParent() + File.separator + GENERATED_PATH + File.separator + file).getFile()
						.getAbsolutePath();
	}

	public String getDestinationFile(String file) throws IOException {
		Resource resource = new FileSystemResource(getPath());
		return new FileSystemResource(
				resource.getFile().getParent() + File.separator + GENERATED_PATH + File.separator + file).getFile()
						.getAbsolutePath();
	}

	private String getPath() {
		String pth = null;
		if (OSValidator.isUnix()) {
			pth = L_PATH;
		} else {
			pth = PATH;
		}
		return pth;
	}
}
