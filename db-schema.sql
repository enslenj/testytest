create database if not exists htnu18app;

use htnu18app;

drop user if exists 'htnu18app'@'%';
create user 'htnu18app'@'%' identified by 'htnu18app';
grant all on htnu18app.* to 'htnu18app'@'%';

drop table if exists patient;
create table patient (
    id int not null auto_increment primary key,
    patIdHash char(64) unique not null
);

drop table if exists home_bp_reading;
create table home_bp_reading (
    id int not null auto_increment primary key,
    patId int not null,
    systolic int not null,
    diastolic int not null,
    pulse int not null,
    readingDate datetime not null,
    followedInstructions tinyint(1) not null,
    createdDate datetime not null
);

create index idxPatId on home_bp_reading (patId);

drop table if exists goal;
create table goal (
  id int not null auto_increment primary key,
  patId int not null,
  extGoalId varchar(100) not null,
  referenceSystem varchar(100) not null,
  referenceCode varchar(100) not null,
  goalText varchar(255) not null,
  systolicTarget int,
  diastolicTarget int,
  targetDate date not null,
  createdDate datetime not null
);

create unique index idxPatGoalId on goal (patId, extGoalId);

drop table if exists goal_history;
create table goal_history (
    id int not null auto_increment primary key,
    goalId int not null,
    achievementStatus varchar(20) not null,
    createdDate datetime not null
);

create index idxGoalId on goal_history (goalId);

drop table if exists counseling;
create table counseling (
    id int not null auto_increment primary key,
    patId int not null,
    extCounselingId varchar(100) not null,
    referenceSystem varchar(100) not null,
    referenceCode varchar(100) not null,
    counselingText varchar(255) not null,
    createdDate datetime not null
);

create index idxPatCounselingId on counseling (patId, extCounselingId);