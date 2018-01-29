CREATE TABLE `locks` (
   `application_id` varchar(255) NOT NULL,
   `expiration_date` datetime DEFAULT NULL,
   `unique_id` varchar(255) DEFAULT NULL,
   `lockVer` int(11) NOT NULL DEFAULT '0',
   PRIMARY KEY (`application_id`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8

 CREATE TABLE `oauth_tokens` (
   `TYPE` varchar(31) NOT NULL,
   `ID` varchar(255) NOT NULL,
   `NUMBER_OF_TIMES_USED` int(11) DEFAULT NULL,
   `CREATION_TIME` datetime DEFAULT NULL,
   `EXPIRATION_POLICY` longblob NOT NULL,
   `LAST_TIME_USED` datetime DEFAULT NULL,
   `PREVIOUS_LAST_TIME_USED` datetime DEFAULT NULL,
   `AUTHENTICATION` longblob NOT NULL,
   `SERVICE` longblob NOT NULL,
   PRIMARY KEY (`ID`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8

 CREATE TABLE `ticketgrantingticket` (
   `TYPE` varchar(31) NOT NULL,
   `ID` varchar(255) NOT NULL,
   `NUMBER_OF_TIMES_USED` int(11) DEFAULT NULL,
   `CREATION_TIME` datetime DEFAULT NULL,
   `EXPIRATION_POLICY` longblob NOT NULL,
   `LAST_TIME_USED` datetime DEFAULT NULL,
   `PREVIOUS_LAST_TIME_USED` datetime DEFAULT NULL,
   `AUTHENTICATION` longblob NOT NULL,
   `EXPIRED` bit(1) NOT NULL,
   `PROXIED_BY` longblob,
   `SERVICES_GRANTED_ACCESS_TO` longblob NOT NULL,
   `ticketGrantingTicket_ID` varchar(255) DEFAULT NULL,
   PRIMARY KEY (`ID`),
   KEY `FKiqyu3qw2fxf5qaqin02mox8r4` (`ticketGrantingTicket_ID`),
   CONSTRAINT `FKiqyu3qw2fxf5qaqin02mox8r4` FOREIGN KEY (`ticketGrantingTicket_ID`) REFERENCES `ticketgrantingticket` (`ID`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8

 CREATE TABLE `serviceticket` (
   `TYPE` varchar(31) NOT NULL,
   `ID` varchar(255) NOT NULL,
   `NUMBER_OF_TIMES_USED` int(11) DEFAULT NULL,
   `CREATION_TIME` datetime DEFAULT NULL,
   `EXPIRATION_POLICY` longblob NOT NULL,
   `LAST_TIME_USED` datetime DEFAULT NULL,
   `PREVIOUS_LAST_TIME_USED` datetime DEFAULT NULL,
   `FROM_NEW_LOGIN` bit(1) NOT NULL,
   `TICKET_ALREADY_GRANTED` bit(1) NOT NULL,
   `SERVICE` longblob NOT NULL,
   `ticketGrantingTicket_ID` varchar(255) DEFAULT NULL,
   PRIMARY KEY (`ID`),
   KEY `FK60oigifivx01ts3n8vboyqs38` (`ticketGrantingTicket_ID`),
   CONSTRAINT `FK60oigifivx01ts3n8vboyqs38` FOREIGN KEY (`ticketGrantingTicket_ID`) REFERENCES `ticketgrantingticket` (`ID`)
 ) ENGINE=InnoDB DEFAULT CHARSET=utf8