INSERT INTO user_ranks (id, rank_name, rank_min_amount, rank_max_amount, point_rate)
VALUES
    (1, 'GENERAL', 0, 100000, 0.01),  -- 10만원 이하, 1% 포인트 적립
    (2, 'ROYAL', 100000, 200000, 0.02),  -- 10만원 ~ 20만원, 2% 포인트 적립
    (3, 'GOLD', 200000, 300000, 0.03),  -- 20만원 ~ 30만원, 3% 포인트 적립
    (4, 'PLATINUM', 300000, NULL, 0.03);  -- 30만원 이상, 3% 포인트 적립


INSERT INTO roles (role_name) VALUES ('ROLE_ADMIN');
INSERT INTO roles (role_name) VALUES ('ROLE_USER');

INSERT INTO users (
    is_oauth, user_birthday, user_signup_date, user_last_login, id, user_name, user_email, user_phone_number, user_pwd, user_status
) value (true, '2024-12-22', '2024-12-23', null, 'apple', '홍길동', 'email@gmail.com','01012345678',
         '$2b$12$rrIioTXmwjsZewl2323Z8OHI0GOfak3MvIAUnISTR5Yyf3S7eRk7y', 'ACTIVE');

INSERT INTO users (
    is_oauth, user_birthday, user_signup_date, user_last_login, id, user_name, user_email, user_phone_number, user_pwd, user_status
) value (false, '2024-12-22', '2024-12-23', null, 'hyewon', '혜원', 'example@gmail.com','01012345678',
         '$2b$12$rrIioTXmwjsZewl2323Z8OHI0GOfak3MvIAUnISTR5Yyf3S7eRk7y', 'ACTIVE');

insert into user_roles(role_id, user_id) VALUE(2, 'apple');



INSERT INTO point_policies (point_policy_name, point_policy_type, point_policy_value) VALUES
                                                                                          ('결제', 'PERCENTAGE', 0.5),
                                                                                          ('회원가입', 'AMOUNT', 5000),
                                                                                          ('리뷰작성', 'AMOUNT', 200),
                                                                                          ('리뷰작성(사진 포함)', 'AMOUNT', 500);


-- 주문 1 (주문 상태 필수 주석 처리)
INSERT INTO orders (id, preferred_delivery_date, delivery_fee, order_time, total_amount, point_amount, name, order_phone, order_email, user_id)
VALUES
    (1, '2024-12-30', 3000, '2024-12-25 12:00:00', 50000, 1000, 'Book Order 1', '010-1234-5678', 'apple@example.com', 'apple'),
    (2, '2024-12-31', 2000, '2024-12-26 14:30:00', 40000, 500, 'Book Order 2', '010-8765-4321', 'apple@example.com', 'apple');

-- Shipment 필수 주석 처리

INSERT INTO publishers (name)
VALUES ('Penguin Books');

insert into series (name)
values ('test name');

insert into books (title, regular_price, sale_price, is_sale, explanation, publisher_id, series_id, packaging, publish_date, isbn, stock, contents) values
                                                                                                                                                        ('test title', 11000, 10000, TRUE, 'test desc', 1, 1, FALSE, '2024-12-12', '1234567890123', 100, 'sample contents'),
                                                                                                                                                        ('test title2', 21000, 20000, TRUE, 'test desc2', 1, 1, FALSE, '2024-12-12', '1234567890124', 100, 'sample contents2'),
                                                                                                                                                        ('test title3', 31000, 30000, TRUE, 'test desc3', 1, 1, FALSE, '2024-12-12', '1234567890125', 100, 'sample contents3');

INSERT INTO order_details (id, order_id, amount_detail, quantity, coupon_id, book_id, refund_or_cancel_date) VALUES
-- 주문 1의 상세 데이터 (주문 상태 필수 주석 처리)
(1, 1, 33000, 1,101, 1, NULL),  -- Clean Code
(2, 1, 45000, 1, 102, 2, NULL), -- Effective Java
-- 주문 2의 상세 데이터
(3, 2, 55000, 1, NULL, 3, NULL);  -- Design Patterns




INSERT INTO point_histories (id, point_amount, point_Type, transaction_date, point_policy_id, user_id, order_id) VALUES
-- 포인트 적립 성공 사례
(1, 1000, 'EARNED', '2024-12-25 12:00:00', 1, 'apple', 1),
-- 포인트 사용 사례
(2, -500, 'USED', '2024-12-26 10:00:00', 2, 'apple', null),
-- 포인트 사용 취소 사례
(3, 500, 'USED_CANCEL', '2024-12-27 14:00:00',3, 'apple', null),
-- 포인트 적립 대기 사례
(4, 300, 'EARNED', '2024-12-28 09:00:00', 2, 'apple', null),
-- 포인트 적립 성공 추가 사례
(5, 1500, 'EARNED', '2024-12-29 08:00:00',  1, 'apple', 2),
(6, 1500, 'EARNED', '2024-12-29 08:00:00',  1, 'hyewon', null);
