package org.gulash;

/**
 * Простая модель пользователя (POJO).
 * Используется для демонстрации сериализации и десериализации JSON при помощи Jackson.
 */
public class User {
    /** Уникальный идентификатор пользователя. */
    private int id;
    /** Имя пользователя. */
    private String name;

    /**
     * Конструктор по умолчанию. Необходим для корректной работы Jackson при десериализации.
     */
    public User() {}

    /**
     * Конструктор со всеми полями. Позволяет удобно создавать объекты в тестах.
     * @param id Уникальный идентификатор пользователя
     * @param name Имя пользователя
     */
    public User(int id, String name) {
        this.id = id;
        this.name = name;
    }

    /** @return Возвращает уникальный идентификатор пользователя. */
    public int getId() { return id; }
    /** @param id Устанавливает уникальный идентификатор пользователя. */
    public void setId(int id) { this.id = id; }
    /** @return Возвращает имя пользователя. */
    public String getName() { return name; }
    /** @param name Устанавливает имя пользователя. */
    public void setName(String name) { this.name = name; }

    /**
     * Сравнивает текущего пользователя с другим объектом.
     * Необходим для корректной работы assertEquals в тестах.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && java.util.Objects.equals(name, user.name);
    }

    /**
     * Генерирует хэш-код объекта.
     */
    @Override
    public int hashCode() {
        return java.util.Objects.hash(id, name);
    }
}
