package ru.yandex.practicum.intershop.model;

/**
 * Структура для организации разбиения на страницы
 * @param pageNumber    номер текущей страницы (по умолчанию, 1)
 * @param pageSize      максимальное число постов на странице (по умолчанию, 10)
 * @param hasNext       можно ли пролистнуть вперед
 * @param hasPrevious   можно ли пролистнуть назад
 */
public record Paging (
        long       pageNumber,
        long       pageSize,
        boolean    hasNext,
        boolean    hasPrevious
)
{}

