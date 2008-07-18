package com.intellij.diagnostic;

import com.intellij.openapi.application.ApplicationNamesInfo;
import com.intellij.openapi.ui.DialogWrapper;
import com.intellij.openapi.ui.Messages;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;

public class OutOfMemoryDialog extends DialogWrapper {
  public enum MemoryKind {
    HEAP, PERM_GEN
  }

  private MemoryKind myMemoryKind;

  private JPanel myContentPane;
  private JLabel myMessageLabel;
  private JTextField myHeapSizeField;
  private JTextField myPermGenSizeField;
  private JLabel myHeapCurrentValueLabel;
  private JLabel myPermGenCurrentValueLabel;
  private JLabel myHeapSizeLabel;
  private JLabel myPermGenSizeLabel;
  private JLabel myHeapUnitsLabel;
  private JLabel myPermGenUnitsLabel;
  private JLabel mySettingsFileHintLabel;
  private Action myIgnoreAction;
  private Action myShutdownAction;

  public OutOfMemoryDialog(MemoryKind memoryKind) {
    super(false);
    myMemoryKind = memoryKind;
    setTitle(DiagnosticBundle.message("diagnostic.out.of.memory.title"));

    myMessageLabel.setIcon(Messages.getErrorIcon());
    myMessageLabel.setText(DiagnosticBundle.message(
        "diagnostic.out.of.memory.error",
        memoryKind == MemoryKind.HEAP ? VMOptions.XMX_OPTION_NAME : VMOptions.PERM_GEN_OPTION_NAME,
        ApplicationNamesInfo.getInstance().getProductName()));

    mySettingsFileHintLabel.setText(DiagnosticBundle.message("diagnostic.out.of.memory.willBeSavedTo",
                                                             VMOptions.getSettingsFilePath()));

    myIgnoreAction = new AbstractAction(DiagnosticBundle.message("diagnostic.out.of.memory.ignore")) {
      public void actionPerformed(ActionEvent e) {
        save();
        close(0);
      }
    };

    myShutdownAction = new AbstractAction(DiagnosticBundle.message("diagnostic.out.of.memory.shutdown")) {
      public void actionPerformed(ActionEvent e) {
        save();
        System.exit(0);
      }
    };
    myShutdownAction.putValue(DialogWrapper.DEFAULT_ACTION, true);

    configControls(VMOptions.XMX_OPTION_NAME,
                   VMOptions.readXmx(),
                   memoryKind == MemoryKind.HEAP,
                   myHeapSizeLabel,
                   myHeapSizeField,
                   myHeapUnitsLabel,
                   myHeapCurrentValueLabel);
    
    configControls(VMOptions.PERM_GEN_OPTION_NAME,
                   VMOptions.readMaxPermGen(),
                   memoryKind == MemoryKind.PERM_GEN,
                   myPermGenSizeLabel,
                   myPermGenSizeField,
                   myPermGenUnitsLabel,
                   myPermGenCurrentValueLabel);

    init();
  }

  private void configControls(String optionName,
                              int value,
                              boolean highlight,
                              JLabel sizeLabel,
                              JTextField sizeField,
                              JLabel unitsLabel,
                              JLabel currentValueLabel) {
    sizeLabel.setText(optionName);

    String formatted = value == -1
           ? DiagnosticBundle.message("diagnostic.out.of.memory.currentValue.unknown")
           : String.valueOf(value);
    sizeField.setText(formatted);
    currentValueLabel.setText(DiagnosticBundle.message("diagnostic.out.of.memory.currentValue", formatted));

    if (highlight) {
      sizeLabel.setForeground(Color.RED);
      sizeField.setForeground(Color.RED);
      unitsLabel.setForeground(Color.RED);
      currentValueLabel.setForeground(Color.RED);
    }
  }

  private void save() {
    try {
      VMOptions.writeXmx(Integer.parseInt(myHeapSizeField.getText()));
    }
    catch (NumberFormatException e) {
    }

    try {
      VMOptions.writeMaxPermGen(Integer.parseInt(myPermGenSizeField.getText()));
    }
    catch (NumberFormatException e) {
    }
  }

  protected JComponent createCenterPanel() {
    return myContentPane;
  }

  protected Action[] createActions() {
    return new Action[]{myShutdownAction, myIgnoreAction};
  }

  @Override
  public JComponent getPreferredFocusedComponent() {
    return myMemoryKind == MemoryKind.HEAP ? myHeapSizeField : myPermGenSizeField;
  }
}
