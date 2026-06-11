update users
set password_hash = '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiRIG7Q7NQxwZ0pniS3pSkeCZMt2rtW'
where email in (
  'collector@archivebid.test',
  'curator@archivebid.test',
  'dealer@archivebid.test'
);

insert into users (email, password_hash, role)
select 'admin@archivebid.test', '$2a$10$7EqJtq98hPqEX7fNZaFWoOHiRIG7Q7NQxwZ0pniS3pSkeCZMt2rtW', 'ADMIN'
where not exists (select 1 from users where email = 'admin@archivebid.test');
