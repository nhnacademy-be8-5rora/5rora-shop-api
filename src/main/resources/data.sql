INSERT INTO users (
    id,
    user_pwd,
    user_name,
    user_birthday,
    user_phone_number,
    user_email,
    user_status,
    user_last_login,
    user_signup_date,
    is_oauth
)
VALUES (
           'user123',
           'password123',
           'John Doe',
           '1990-01-01',
           '010-1234-5678',
           'john.doe@example.com',
           'ACTIVE',
           '2024-12-12 14:30:00',
           '2024-12-12',
           TRUE
       ),
       (
       'user1234',
       'password123',
       'John Doe',
       '1990-01-01',
       '010-1234-5678',
       'john.doe@example.com',
       'ACTIVE',
       '2024-12-12 14:30:00',
       '2024-12-12',
       TRUE
   );

INSERT INTO roles (role_name) VALUES ('ROLE_ADMIN');
INSERT INTO roles (role_name) VALUES ('ROLE_USER');

INSERT INTO user_roles (user_id, role_id) VALUES ('user123', 1);

-- ///////////////////////// book ////////////////////////////

INSERT INTO publishers (name)
VALUES ('Penguin Books');

insert into serieses (name)
values ('test name');

insert into books (title, regular_price, sale_price, is_sale, explanation, publisher_id, series_id, PACKAGING, PUBLISH_DATE)
values ('test title', 10000, 9000, TRUE, 'test desc', 1, 1, FALSE, '2024-12-12');

insert into books (title, regular_price, sale_price, is_sale, explanation, publisher_id, series_id, PACKAGING, PUBLISH_DATE)
values ('test title2', 10000, 9000, TRUE, 'test desc2', 1, 1, FALSE, '2024-12-12');