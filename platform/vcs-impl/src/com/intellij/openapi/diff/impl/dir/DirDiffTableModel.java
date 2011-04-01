/*
 * Copyright 2000-2011 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.intellij.openapi.diff.impl.dir;

import com.intellij.openapi.progress.ProgressIndicator;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.vfs.VirtualFile;
import com.intellij.util.containers.HashSet;
import org.jetbrains.annotations.Nullable;

import javax.swing.table.AbstractTableModel;
import java.io.IOException;
import java.util.*;

/**
 * @author Konstantin Bulenkov
 */
public class DirDiffTableModel extends AbstractTableModel {
  private final Project myProject;
  final List<DirDiffElement> myElements = new ArrayList<DirDiffElement>();

  public DirDiffTableModel(Project project, VirtualFile src, VirtualFile trg, ProgressIndicator indicator) {
    myProject = project;
    loadModel(src, trg, indicator);
  }

  private void loadModel(VirtualFile src, VirtualFile trg, ProgressIndicator indicator) {
    final HashSet<String> files = new HashSet<String>();
    scan("", src, files, indicator, true);
    scan("", trg, files, indicator, true);
    final ArrayList<String> pathes = new ArrayList<String>(files);
    Collections.sort(pathes, new Comparator<String>() {
      @Override
      public int compare(String o1, String o2) {
        final boolean b1 = o1.endsWith("/");
        final boolean b2 = o2.endsWith("/");
        final String[] dirs1 = o1.split("/");
        final String[] dirs2 = o2.split("/");
        for (int i = 0; i < Math.min(dirs1.length, dirs2.length); i++) {
          final int cmp = dirs1[i].toLowerCase().compareTo(dirs2[i].toLowerCase());
          if (cmp != 0) return cmp;
        }
        return dirs1.length - dirs2.length;
      }
    });
    for (String path : pathes) {
      final VirtualFile srcFile = src.findFileByRelativePath(path);
      final VirtualFile trgFile = trg.findFileByRelativePath(path);
      if (srcFile == null && trgFile != null) {
        myElements.add(DirDiffElement.createTargetOnly(trgFile));
      } else if (srcFile != null && trgFile == null) {
        myElements.add(DirDiffElement.createSourceOnly(srcFile));
      } else if (srcFile != null && trgFile != null) {
        indicator.setText2("Comparing " + path);
        if (srcFile.isDirectory() && trgFile.isDirectory()) {
          myElements.add(DirDiffElement.createDirElement(srcFile, trgFile, path));
        } else if (srcFile.isDirectory() && !trgFile.isDirectory()) {
          myElements.add(DirDiffElement.createDirElement(srcFile, null, path));
          myElements.add(DirDiffElement.createTargetOnly(trgFile));
        } else if (!srcFile.isDirectory() && trgFile.isDirectory()) {
          myElements.add(DirDiffElement.createDirElement(null, trgFile, path));
          myElements.add(DirDiffElement.createSourceOnly(srcFile));
        } else if (!isEqual(srcFile, trgFile)) {
            myElements.add(DirDiffElement.createChange(srcFile, trgFile));
        }
      }
    }
  }

  private static boolean isEqual(VirtualFile file1, VirtualFile file2) {
    if (file1.isDirectory() || file2.isDirectory()) return false;
    if (file1.getLength() != file2.getLength()) return false;
    try {
      return Arrays.equals(file1.contentsToByteArray(), file2.contentsToByteArray());
    }
    catch (IOException e) {
      return false;
    }
  }

  private static void scan(String prefix, VirtualFile file, HashSet<String> files, ProgressIndicator indicator, boolean isRoot) {
    if (file.isDirectory()) {
      indicator.setText2(file.getPath());
      String p = isRoot ? "" : prefix + file.getName() + "/";
      if (!isRoot) {
        files.add(p);
      }
      for (VirtualFile f : file.getChildren()) {
        scan(p, f, files, indicator, false);
      }
    } else {
      files.add(prefix + file.getName());
    }
  }

  @Override
  public int getRowCount() {
    return myElements.size();
  }

  @Override
  public int getColumnCount() {
    return 5;
  }

  @Nullable
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    final DirDiffElement element = myElements.get(rowIndex);
    if (element.isSeparator()) {
      return columnIndex == 0 ? element.getName() : null;
    }
    switch (columnIndex) {
      case 0: return element.getSourceName();
      case 1: return element.getSourceSize();
      case 2: return "";
      case 3: return element.getTargetSize();
      case 4: return element.getTargetName();
    }
    return null;
  }
}
