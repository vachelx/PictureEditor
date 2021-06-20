package com.vachel.editor;

public interface IEditSave {
    void onSaveSuccess(String savePath);

    void onSaveFailed();
}
