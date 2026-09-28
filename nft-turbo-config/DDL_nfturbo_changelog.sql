# 2026-07-21 系列表重命名 + 状态补充 PENDING_REVIEW
RENAME TABLE `collection_series` TO `series`;
ALTER TABLE `series` MODIFY COLUMN `state` varchar(32) NOT NULL DEFAULT 'INIT' COMMENT '状态：INIT/PENDING_REVIEW/SUCCEED/REMOVED';

# 2026-07-18 星尘每日闪购：新增星尘包配置表


CREATE TABLE `star_daily_flash` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增主键）',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '最后更新时间',
    `name` varchar(128) NOT NULL COMMENT '星尘包名称（如"星尘包·小"）',
    `cover` varchar(512) DEFAULT '' COMMENT '封面图URL（固定，不上传）',
    `star_amount` bigint NOT NULL COMMENT '含星尘数量',
    `price` decimal(18,6) NOT NULL COMMENT '价格（元）',
    `quantity` bigint NOT NULL COMMENT '总发行量',
    `detail` text COMMENT '详情描述',
    `saleable_inventory` bigint NOT NULL COMMENT '可售库存',
    `creator_id` varchar(32) NOT NULL COMMENT '创建者ID',
    `state` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/INACTIVE',
    `sale_time` datetime DEFAULT NULL COMMENT '开售时间',
    `deleted` int DEFAULT 0 COMMENT '是否逻辑删除',
    `lock_version` int DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='星尘每日闪购配置表';


# 2026-07-20 合成系统：配方表 + 合成流水表

CREATE TABLE `synthesis_recipe` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `gmt_create` datetime NOT NULL,
    `gmt_modified` datetime NOT NULL,
    `series_id` bigint NOT NULL COMMENT '所属系列',
    `name` varchar(128) NOT NULL COMMENT '配方名称',
    `source_rarity` varchar(32) NOT NULL COMMENT '材料稀有度',
    `target_rarity` varchar(32) NOT NULL COMMENT '产物稀有度',
    `card_count` int NOT NULL COMMENT '需烧卡数量',
    `star_cost` bigint NOT NULL COMMENT '消耗星尘数量',
    `state` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    `creator_id` varchar(128) DEFAULT NULL COMMENT '创建者',
    `deleted` int DEFAULT 0,
    `lock_version` int DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_series` (`series_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合成配方表';

CREATE TABLE `synthesis_stream` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `gmt_create` datetime NOT NULL,
    `gmt_modified` datetime NOT NULL,
    `identifier` varchar(128) NOT NULL,
    `user_id` varchar(128) NOT NULL,
    `recipe_id` bigint NOT NULL,
    `series_id` bigint NOT NULL,
    `source_rarity` varchar(32) NOT NULL,
    `target_rarity` varchar(32) NOT NULL,
    `card_count` int NOT NULL,
    `star_cost` bigint NOT NULL,
    `forge_value_before` bigint DEFAULT NULL,
    `forge_value_after` bigint DEFAULT NULL,
    `state` varchar(32) NOT NULL DEFAULT 'SUBMITTED',
    `product_held_id` bigint DEFAULT NULL,
    `material_ids` text COMMENT '材料卡ID列表',
    `deleted` int DEFAULT 0,
    `lock_version` int DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_identifier` (`identifier`),
    KEY `idx_user` (`user_id`),
    KEY `idx_series` (`series_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='合成流水表';


# 2026-07-20 盲盒奖池改造：条目加显式概率 + 盲盒加概率公示标识

ALTER TABLE blind_box_item
    ADD COLUMN `probability` decimal(5,2) DEFAULT NULL COMMENT '概率（%，NULL=按quantity占比）',
    ADD COLUMN `display_order` int DEFAULT 0 COMMENT '展示排序';

ALTER TABLE blind_box
    ADD COLUMN `probability_public` tinyint NOT NULL DEFAULT 1 COMMENT '概率是否公示';


# 2026-07-18 盲盒系统：盲盒加系列，条目加藏品关联+系列+星尘

ALTER TABLE blind_box
  ADD COLUMN series_id bigint NULL COMMENT '所属系列' AFTER creator_id;

ALTER TABLE blind_box_item
  ADD COLUMN collection_id bigint NULL COMMENT '关联藏品（NULL=星尘包）' AFTER rarity,
  ADD COLUMN series_id bigint NULL COMMENT '冗余系列（从collection带出）' AFTER collection_id,
  ADD COLUMN star_amount bigint NULL COMMENT '星尘数量（NULL=藏品卡，>0=星尘包）' AFTER series_id;


# 2026-07-18 星尘账户基础：新增星尘账户表和流水表

CREATE TABLE `star_account` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增主键）',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '最后更新时间',
    `user_id` varchar(32) NOT NULL COMMENT '用户ID',
    `balance` bigint NOT NULL DEFAULT 0 COMMENT '当前星尘余额',
    `total_earned` bigint NOT NULL DEFAULT 0 COMMENT '累计获得星尘',
    `total_spent` bigint NOT NULL DEFAULT 0 COMMENT '累计消耗星尘',
    `state` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT '状态：ACTIVE/FROZEN',
    `deleted` int DEFAULT 0 COMMENT '是否逻辑删除，0为未删除，非0为已删除',
    `lock_version` int DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='星尘账户表';

CREATE TABLE `star_account_stream` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增主键）',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '最后更新时间',
    `identifier` varchar(128) NOT NULL COMMENT '幂等号',
    `user_id` varchar(32) NOT NULL COMMENT '用户ID',
    `change_type` varchar(32) NOT NULL COMMENT '变更类型：PURCHASE/BOX_DROP/GIFT/TASK/AIRDROP/SPEND',
    `change_amount` bigint NOT NULL COMMENT '变更数量（正为获得，负为消耗）',
    `balance_after` bigint NOT NULL COMMENT '变更后余额',
    `biz_no` varchar(128) DEFAULT NULL COMMENT '业务单据号（订单ID/盲盒ItemID等）',
    `biz_type` varchar(32) DEFAULT NULL COMMENT '业务类型',
    `extend_info` varchar(1024) DEFAULT NULL COMMENT '扩展信息',
    `deleted` int DEFAULT 0 COMMENT '是否逻辑删除，0为未删除，非0为已删除',
    `lock_version` int DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_identifier` (`identifier`),
    KEY `idx_user_time` (`user_id`, `gmt_create`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='星尘账户流水表';


# 2026-07-20 星尘库存流水表

CREATE TABLE `star_inventory_stream` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增主键）',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '最后更新时间',
    `identifier` varchar(128) NOT NULL COMMENT '幂等号',
    `star_id` bigint NOT NULL COMMENT '星尘包ID',
    `price` decimal(18,6) DEFAULT NULL COMMENT '价格',
    `quantity` bigint DEFAULT NULL COMMENT '发行总量',
    `saleable_inventory` bigint DEFAULT NULL COMMENT '可售库存',
    `state` varchar(128) DEFAULT NULL COMMENT '状态',
    `changed_quantity` bigint NOT NULL COMMENT '变更数量',
    `stream_type` varchar(32) NOT NULL COMMENT '流水类型：INVENTORY_INCREASE/INVENTORY_DECREASE',
    `extend_info` varchar(512) DEFAULT NULL COMMENT '扩展信息',
    `deleted` int DEFAULT 0 COMMENT '是否逻辑删除，0为未删除，非0为已删除',
    `lock_version` int DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_identifier` (`identifier`, `stream_type`, `star_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='星尘库存流水表';


# 2026-07-17 合成玩法P0：新增系列表，collection/held_collection/collection_stream 扩展稀有度/系列/铸造值/获取途径

CREATE TABLE `collection_series` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT COMMENT '主键ID（自增主键）',
    `gmt_create` datetime NOT NULL COMMENT '创建时间',
    `gmt_modified` datetime NOT NULL COMMENT '最后更新时间',
    `name` varchar(128) NOT NULL COMMENT '系列名称',
    `cover` varchar(512) DEFAULT NULL COMMENT '系列封面',
    `description` text COMMENT '系列故事/介绍',
    `state` varchar(32) NOT NULL DEFAULT 'INIT' COMMENT '状态：INIT/PREVIEW/ON_SALE/SOLD_OUT/FINISHED',
    `creator_id` varchar(128) DEFAULT NULL COMMENT '创建者',
    `deleted` int DEFAULT 0 COMMENT '是否逻辑删除，0为未删除，非0为已删除',
    `lock_version` int DEFAULT 0 COMMENT '乐观锁版本号',
    PRIMARY KEY (`id`),
    KEY `idx_state` (`state`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='藏品系列表';

ALTER TABLE `collection`
	ADD COLUMN `rarity` varchar(32) NULL COMMENT '稀有度：COMMON/RARE/EPIC/LEGENDARY/UNIQUE/MYTHICAL' AFTER `detail`,
	ADD COLUMN `series_id` bigint NULL COMMENT '所属系列id' AFTER `rarity`,
	ADD COLUMN `forge_value` bigint NULL COMMENT '铸造值' AFTER `series_id`,
	ADD COLUMN `obtain_type` varchar(32) NOT NULL DEFAULT 'DIRECT_SALE' COMMENT '获取途径：DIRECT_SALE/SYNTHESIS_ONLY/BOX_ONLY' AFTER `forge_value`,
	ADD INDEX `idx_series` (`series_id`)
;

ALTER TABLE `held_collection`
	ADD COLUMN `forge_value` bigint NULL COMMENT '铸造值快照（创建时从藏品带入）' AFTER `rarity`,
	ADD INDEX `idx_collection_state` (`collection_id`, `state`)
;

ALTER TABLE `collection_stream`
	ADD COLUMN `rarity` varchar(32) NULL COMMENT '稀有度' AFTER `detail`,
	ADD COLUMN `series_id` bigint NULL COMMENT '所属系列id' AFTER `rarity`,
	ADD COLUMN `forge_value` bigint NULL COMMENT '铸造值' AFTER `series_id`,
	ADD COLUMN `obtain_type` varchar(32) NULL COMMENT '获取途径' AFTER `forge_value`
;

ALTER TABLE `collection_snapshot`
	ADD COLUMN `rarity` varchar(32) NULL COMMENT '稀有度' AFTER `detail`,
	ADD COLUMN `series_id` bigint NULL COMMENT '所属系列id' AFTER `rarity`,
	ADD COLUMN `forge_value` bigint NULL COMMENT '铸造值' AFTER `series_id`,
	ADD COLUMN `obtain_type` varchar(32) NULL COMMENT '获取途径' AFTER `forge_value`
;


# 2024-08-31 trade_order 表新增reverse_buyer_id

ALTER TABLE `trade_order_0000`
	ADD COLUMN `reverse_buyer_id` varchar(32) NULL COMMENT '逆序的买家ID' AFTER `buyer_id`,
	ADD KEY `idx_rvbuyer_state`(`reverse_buyer_id`,`order_state`,`gmt_create`) USING BTREE
;

ALTER TABLE `trade_order_0001`
	ADD COLUMN `reverse_buyer_id` varchar(32) NULL COMMENT '逆序的买家ID' AFTER `buyer_id`,
	ADD KEY `idx_rvbuyer_state`(`reverse_buyer_id`,`order_state`,`gmt_create`) USING BTREE
;

ALTER TABLE `trade_order_0002`
	ADD COLUMN `reverse_buyer_id` varchar(32) NULL COMMENT '逆序的买家ID' AFTER `buyer_id`,
	ADD KEY `idx_rvbuyer_state`(`reverse_buyer_id`,`order_state`,`gmt_create`) USING BTREE
;

ALTER TABLE `trade_order_0003`
	ADD COLUMN `reverse_buyer_id` varchar(32) NULL COMMENT '逆序的买家ID' AFTER `buyer_id`,
	ADD KEY `idx_rvbuyer_state`(`reverse_buyer_id`,`order_state`,`gmt_create`) USING BTREE
;

update trade_order_0000 set `reverse_buyer_id`  = REVERSE(`buyer_id` );
update trade_order_0001 set `reverse_buyer_id`  = REVERSE(`buyer_id` );
update trade_order_0003 set `reverse_buyer_id`  = REVERSE(`buyer_id` );
update trade_order_0002 set `reverse_buyer_id`  = REVERSE(`buyer_id` );


# 2024-08-25 新增refund_order表

/******************************************/
/*   DatabaseName = nfturbo   */
/*   TableName = refund_order   */
/******************************************/
CREATE TABLE `refund_order` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '主键ID',
  `gmt_create` datetime NOT NULL COMMENT '创建时间',
  `gmt_modified` datetime NOT NULL COMMENT '修改时间',
  `refund_order_id` varchar(32) NOT NULL COMMENT '支付单号',
  `identifier` varchar(128) NOT NULL COMMENT '幂等号',
  `pay_order_id` varchar(32) NOT NULL COMMENT '支付单号',
  `pay_channel_stream_id` varchar(64) DEFAULT NULL COMMENT '支付的渠道流水号',
  `paid_amount` decimal(18,6) DEFAULT NULL COMMENT '已支付金额',
  `payer_id` varchar(32) NOT NULL COMMENT '付款方iD',
  `payer_type` varchar(32) NOT NULL COMMENT '付款方类型',
  `payee_id` varchar(32) NOT NULL COMMENT '收款方id',
  `payee_type` varchar(32) NOT NULL COMMENT '收款方类型',
  `apply_refund_amount` decimal(18,6) NOT NULL COMMENT '申请退款金额',
  `refunded_amount` decimal(18,6) DEFAULT NULL COMMENT '退款成功金额',
  `refund_channel_stream_id` varchar(64) DEFAULT NULL COMMENT '退款的渠道流水号',
  `refund_channel` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款方式',
  `memo` varchar(512) DEFAULT NULL COMMENT '备注',
  `refund_order_state` varchar(64) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci NOT NULL COMMENT '退款单状态',
  `refund_succeed_time` datetime DEFAULT NULL COMMENT '退款成功时间',
  `deleted` tinyint DEFAULT NULL COMMENT '逻辑删除标识',
  `lock_version` int DEFAULT NULL COMMENT '乐观锁版本号',
  PRIMARY KEY (`id`),
  KEY `idx_pay_order` (`pay_order_id`) USING BTREE,
  KEY `uk_identifier` (`identifier`,`pay_order_id`,`refund_channel`),
  KEY `idx_refund_order` (`refund_order_id`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
;
# 2026-07-20 obtainType 从 Collection 主表迁移到 HeldCollection
ALTER TABLE `collection` DROP COLUMN `obtain_type`;
ALTER TABLE `collection_stream` DROP COLUMN `obtain_type`;
ALTER TABLE `collection_snapshot` DROP COLUMN `obtain_type`;
ALTER TABLE `held_collection` ADD COLUMN `obtain_type` varchar(32) NULL COMMENT '获取途径：DIRECT_SALE/BLIND_BOX/SYNTHESIS/TRANSFER/AIRDROP' AFTER `biz_no`;

# 2026-07-22 collection 表恢复 obtain_type，语义改为「可获取途径」（逗号分隔多选）
ALTER TABLE `collection`
    ADD COLUMN `obtain_type` varchar(64) NOT NULL DEFAULT 'DIRECT_SALE'
    COMMENT '可获取途径（逗号分隔）：DIRECT_SALE,BLIND_BOX,SYNTHESIS'
    AFTER `forge_value`;

# 2026-07-23 图鉴系统三张表
CREATE TABLE `user_album_progress` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `gmt_create` datetime NOT NULL,
    `gmt_modified` datetime NOT NULL,
    `user_id` varchar(128) NOT NULL COMMENT '用户ID',
    `series_id` bigint NOT NULL COMMENT '系列ID',
    `collected` text COMMENT '已点亮的collectionId JSON数组',
    `total` int NOT NULL DEFAULT 0 COMMENT '系列藏品总数快照',
    `state` varchar(32) NOT NULL DEFAULT 'COLLECTING' COMMENT 'COLLECTING/COMPLETED',
    `completed_at` datetime DEFAULT NULL,
    `deleted` int DEFAULT 0,
    `lock_version` int DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_series` (`user_id`, `series_id`)
) ENGINE=InnoDB COMMENT='用户图鉴进度表';

CREATE TABLE `album_reward_record` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `gmt_create` datetime NOT NULL,
    `gmt_modified` datetime NOT NULL,
    `user_id` varchar(128) NOT NULL,
    `series_id` bigint NOT NULL,
    `milestone_type` varchar(32) NOT NULL COMMENT 'ALL_N/ALL_N_R/ALL_SR/ALL_UR',
    `reward_type` varchar(32) NOT NULL COMMENT 'STAR/AIRDROP/BOTH',
    `star_amount` bigint DEFAULT 0,
    `airdrop_collection_id` bigint DEFAULT NULL,
    `airdrop_held_id` bigint DEFAULT NULL,
    `deleted` int DEFAULT 0,
    `lock_version` int DEFAULT 0,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_user_series_milestone` (`user_id`, `series_id`, `milestone_type`)
) ENGINE=InnoDB COMMENT='图鉴奖励领取记录';

CREATE TABLE `album_milestone_config` (
    `id` bigint unsigned NOT NULL AUTO_INCREMENT,
    `gmt_create` datetime NOT NULL,
    `gmt_modified` datetime NOT NULL,
    `series_id` bigint NOT NULL COMMENT '所属系列',
    `name` varchar(128) NOT NULL COMMENT '里程碑名称',
    `milestone_type` varchar(32) NOT NULL COMMENT 'ALL_N/ALL_N_R/ALL_SR/ALL_UR',
    `required_count` int NOT NULL COMMENT '需收集卡数',
    `star_reward` bigint DEFAULT 0 COMMENT '星尘奖励',
    `airdrop_collection_id` bigint DEFAULT NULL COMMENT '隐藏款藏品ID',
    `state` varchar(32) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE/DISABLED',
    `deleted` int DEFAULT 0,
    `lock_version` int DEFAULT 0,
    PRIMARY KEY (`id`),
    KEY `idx_series` (`series_id`)
) ENGINE=InnoDB COMMENT='图鉴里程碑配置表';

# 2026-07-24 合成配方扩展第二套方案：指定卡配方
ALTER TABLE `synthesis_recipe`
    ADD COLUMN `recipe_type` varchar(32) NOT NULL DEFAULT 'RARITY_BASED' COMMENT 'RARITY_BASED(稀有度配方)/CARD_BASED(指定卡配方)',
    ADD COLUMN `source_collection_ids` text NULL COMMENT '指定材料藏品ID，逗号分隔（CARD_BASED专用）',
    ADD COLUMN `target_collection_id` bigint NULL COMMENT '指定产物藏品ID（CARD_BASED专用）';
