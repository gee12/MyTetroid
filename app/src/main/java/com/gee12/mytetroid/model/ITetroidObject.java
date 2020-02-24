package com.gee12.mytetroid.model;

/**
 * Общий интерфейс для объектов всех типов в хранилище.
 */
public interface ITetroidObject {

    /**
     * Тип объекта.
     * @return
     */
    int getType();

    /**
     * Заголовок объекта.
     * @return
     */
    String getName();

    /**
     * ID объекта.
     * @return
     */
    String getId();

    /**
     * Зашифрован?
     * @return
     */
    boolean isCrypted();
}
