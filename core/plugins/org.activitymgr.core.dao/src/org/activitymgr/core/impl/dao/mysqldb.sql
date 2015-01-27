drop table if exists CONTRIBUTION;
drop table if exists DURATION;
drop table if exists TASK;
drop table if exists COLLABORATOR;

-- ------------------------------------------------------------
-- Collaborateurs
-- ------------------------------------------------------------
create table COLLABORATOR (
	CLB_ID         integer( 3) not null auto_increment,
	CLB_LOGIN      varchar(20) unique not null,
	CLB_FIRST_NAME varchar(20) not null,
	CLB_LAST_NAME  varchar(20) not null,
 	CLB_IS_ACTIVE  integer( 1) not null,
    index CLB_LOGIN_IDX (CLB_LOGIN),
    constraint CLB_PK primary key (CLB_ID) 
) engine=innodb;

-- ------------------------------------------------------------
-- Taches
-- ------------------------------------------------------------
create table TASK (
	TSK_ID           integer(   4) not null auto_increment,
	TSK_PATH         varchar( 255) not null,
	TSK_NUMBER       varchar(   2) not null,
	TSK_CODE         varchar(  10) not null,
	TSK_NAME         varchar(  50) not null,
	TSK_BUDGET       integer(   8) not null,
	TSK_INITIAL_CONS integer(   8) not null,
	TSK_TODO         integer(   8) not null,
	TSK_COMMENT      text,
    index TSK_PATH_IDX (TSK_PATH),
    index TSK_FULLPATH_IDX (TSK_PATH, TSK_NUMBER),
    index TSK_PATH_CODE_IDX (TSK_PATH, TSK_CODE),
    constraint TSK_PK primary key (TSK_ID),
    constraint TSK_UNIQUE_FULLPATH 
    	unique (TSK_PATH, TSK_NUMBER),
    constraint TSK_UNIQUE_PATH_CODE
    	unique (TSK_PATH, TSK_CODE) 
) engine=innodb;

-- ------------------------------------------------------------
-- Dur�es
-- ------------------------------------------------------------
create table DURATION (
	DUR_ID         integer(3) not null,
	DUR_IS_ACTIVE  integer(1) not null,
    constraint DUR_PK primary key (DUR_ID)
) engine=innodb;

-- ------------------------------------------------------------
-- Taches
-- ------------------------------------------------------------
create table CONTRIBUTION (
	CTB_YEAR          integer(4) not null,
	CTB_MONTH         integer(2) not null,
	CTB_DAY           integer(2) not null,
	CTB_CONTRIBUTOR   integer(3) not null,
	CTB_TASK          integer(3) not null,
	CTB_DURATION      integer(3) not null,
    index CTB_CONTRIBUTOR_IDX (CTB_CONTRIBUTOR),
    index CTB_TASK_IDX (CTB_TASK),
    index CTB_DURATION_IDX (CTB_DURATION),
    constraint CTB_PK primary key (CTB_YEAR, CTB_MONTH, CTB_DAY, CTB_CONTRIBUTOR, CTB_TASK),
    constraint CTB_CONTRIBUTOR_FK foreign key (CTB_CONTRIBUTOR) references COLLABORATOR (CLB_ID),
    constraint CTB_TASK_FK foreign key (CTB_TASK) references TASK (TSK_ID),
    constraint CTB_DURATION_FK foreign key (CTB_DURATION) references DURATION (DUR_ID)
) engine=innodb;
