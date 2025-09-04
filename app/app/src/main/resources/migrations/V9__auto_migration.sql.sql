ALTER TABLE users ADD CONSTRAINT fk_users_role__id FOREIGN KEY ("role") REFERENCES roles(id) ON DELETE CASCADE ON UPDATE RESTRICT;
ALTER TABLE role_permissions ADD CONSTRAINT fk_role_permissions_role__id FOREIGN KEY ("role") REFERENCES roles(id) ON DELETE CASCADE ON UPDATE RESTRICT;
CREATE INDEX discounts_type ON discounts ("type");
CREATE INDEX orders_payment_status ON orders (payment_status);
CREATE INDEX orders_status ON orders (status);
CREATE INDEX discounts_applied_to ON discounts (applied_to);
CREATE INDEX special_offers_type ON special_offers ("type");
