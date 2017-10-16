CREATE TABLE OdbItem (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  item VARCHAR(255) NOT NULL
) ENGINE=InnoDB, CHARSET=utf8;

CREATE TABLE OdbTag (
  itemId VARCHAR(36) NOT NULL,
  tag VARCHAR(255) NOT NULL,
  CONSTRAINT UNIQUE (itemId, tag),
  CONSTRAINT `fk_tag_item` FOREIGN KEY (itemId) REFERENCES OdbItem(id)
) ENGINE=InnoDB, CHARSET=utf8;

CREATE TABLE TSDTVAgent (
  id VARCHAR(36) NOT NULL PRIMARY KEY,
  agentId VARCHAR(100) NOT NULL,
  status ENUM('unregistered', 'registered', 'blacklisted') NOT NULL DEFAULT 'unregistered',
  lastHeartbeatFrom VARCHAR(100),
  CONSTRAINT UNIQUE (agentId),
  INDEX (agentId),
  INDEX (status)
) ENGINE=InnoDB, CHARSET=utf8;