package org.hva.sensei.logger;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class Util {
	public static List<String> getSortedFilenames(File dir, String ext) {
		return getSortedFilenames(dir, null, ext);
	}

	public static List<String> getSortedFilenames(File dir, String sub, String ext) {
		List<String> list = new ArrayList<String>();
		readDirectory(dir, list, "", ext);
		Collections.sort(list, new Comparator<String>() {
			@Override
			public int compare(String object1, String object2) {
				if (object1.compareTo(object2) < 0) {
					return -1;
				} else if (object1.equals(object2)) {
					return 0;
				}
				return 1;
			}

		});
		return list;
	}

	private static void readDirectory(File dir, final List<String> list,
			String parent, String extension) {
		if (dir != null && dir.canRead()) {
			File[] files = dir.listFiles();
			if (files != null) {
				for (File f : files) {
					if (f.getName().toLowerCase().endsWith(extension)) { //$NON-NLS-1$
						list.add(parent + f.getName());
					} else if (f.isDirectory()) {
						readDirectory(f, list, parent + f.getName() + "/",
								extension);
					}
				}
			}
		}
	}
}
