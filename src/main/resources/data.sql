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

insert into user_roles(role_id, user_id) VALUE(2, 'apple');