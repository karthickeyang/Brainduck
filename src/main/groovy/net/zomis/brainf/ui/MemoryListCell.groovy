package net.zomis.brainf.ui

import javafx.scene.control.ListCell
import javafx.scene.control.TextField

/**
 * Created by Simon on 12/5/2015.
 */
class MemoryListCell extends ListCell<String> {

    private TextField textField

    MemoryListCell() {
        super()
        textField = new TextField()
        this.getChildren().add(textField)
    }

    @Override
    protected void updateItem(String item, boolean empty) {
        super.updateItem(item, empty)
        textField.setText(item)
    }
}
