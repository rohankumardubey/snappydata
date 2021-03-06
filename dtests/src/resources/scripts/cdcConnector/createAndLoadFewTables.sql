 DROP TABLE IF EXISTS CLAIM;
 DROP TABLE IF EXISTS staging_CLAIM;
 CREATE EXTERNAL TABLE staging_CLAIM
     USING com.databricks.spark.csv OPTIONS (path ':dataLocation/CLAIM.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
 CREATE TABLE CLAIM (
 PRSN_ID BIGINT NOT NULL,
   CLM_ID BIGINT NOT NULL,
   VER BIGINT NOT NULL,
   CLIENT_ID BIGINT NOT NULL,
   SUBS_ID BIGINT,
   SERVICING_PRVD_ID BIGINT NOT NULL,
   BENE_GRP_ID BIGINT,
   CLM_TYP_REF_ID BIGINT NOT NULL,
   CLM_SUB_TYP_REF_ID BIGINT NOT NULL,
   CLM_RCV_DT DATE,
   CLM_INP_DT DATE,
   LAST_ACT_TS DATE,
   CLM_PD_DT DATE,
   NXT_RVW_DT DATE,
   SERV_FRM_DT DATE,
   SERV_TO_DT DATE,
   PAYEE_PRVD_ID BIGINT,
   CLM_RLS_IND VARCHAR(200),
   CUR_ILLNESS_DT DATE,
   SIMILLAR_ILLNESS_DT DATE,
   PCP_ID BIGINT,
   REFNG_PRVD_ID BIGINT,
   PRE_AUTH_NUM VARCHAR(200),
   PAY_CALC_DT DATE,
   CLM_AI_EOB_IND VARCHAR(200),
   EXPLAIN_CD_ID BIGINT,
   IMG_ADDR VARCHAR(200),
   UNABLE_TO_WRK_FRM DATE,
   UNABLE_TO_WRK_TO DATE,
   HOSP_FRM DATE,
   HOSP_TO DATE,
   LAB_SVIND VARCHAR(200),
   MDCR_RE_SUBM_NUM VARCHAR(200),
   OUT_OF_AREA_IND VARCHAR(200),
   XRAY_IND VARCHAR(200),
   CLM_RECORDS_IND VARCHAR(200),
   CLM_PROC_DT DATE,
   CRTD_FRM_CLM_ID BIGINT,
   ADJ_TO_CLM_ID BIGINT,
   ADJ_FRM_CLM_ID BIGINT,
   NTWK_PRVD_ENT_PRFX VARCHAR(200),
   NTWK_PRVD_PRFX VARCHAR(200),
   NTWK_PRVD_CAP_PRFX VARCHAR(200),
   NON_PARTIC_PRVD_PRFX VARCHAR(200),
   SERV_DEF_PRFX VARCHAR(200),
   PRVD_ACCUM_PRFX VARCHAR(200),
   PRCS_CTRL_AGNT_PRFX VARCHAR(200),
   MOD_PRC_RULES_PRFX VARCHAR(200),
   PRVD_NTWK_ID VARCHAR(200),
   PRVD_AGREE_ID VARCHAR(200),
   MDCR_ASSIGN_IND VARCHAR(200),
   PAY_PRVD_IND VARCHAR(200),
   OTH_BENE_IND VARCHAR(200),
   ACDT_IND VARCHAR(200),
   ACDT_ST VARCHAR(200),
   ACDT_DT DATE,
   ACDT_AMT NUMERIC(38,8),
   PTNT_ACCT_NUM VARCHAR(200),
   PTNT_PD_AMT NUMERIC(38,8),
   CLM_TOT_CHRG NUMERIC(38,8),
   CLM_TOT_PAY NUMERIC(38,8),
   CHK_CYCLE_OVRD_IND VARCHAR(200),
   CLM_INP_METHOD VARCHAR(200),
   CLM_AUD_IND VARCHAR(200),
   EXT_REF_IND VARCHAR(200),
   VLD_FRM_DT DATE NOT NULL,
   VLD_TO_DT DATE,
   SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
   SRC_SYS_REC_ID VARCHAR(150)
 ) USING column OPTIONS(partition_by 'PRSN_ID', buckets '32',key_columns 'CLIENT_ID,PRSN_ID,CLM_ID ' );
 INSERT INTO CLAIM (SELECT * from staging_CLAIM);


 DROP TABLE IF EXISTS AGREEMENT;
 DROP TABLE IF EXISTS staging_AGREEMENT;
 CREATE EXTERNAL TABLE staging_AGREEMENT
  USING com.databricks.spark.csv OPTIONS (path ':dataLocation/AGREEMENT.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
 CREATE TABLE AGREEMENT(AGREE_ID BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  AGREE_CD VARCHAR(200),
  DESCR VARCHAR(200),
  EFF_DT DATE,
  EXPR_DT DATE,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(200) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(200)) USING column OPTIONS(partition_by 'AGREE_ID', buckets '32',key_columns 'CLIENT_ID,AGREE_ID ' ); 
 INSERT INTO AGREEMENT (SELECT * FROM staging_AGREEMENT);

 DROP TABLE IF EXISTS BANK;
 DROP TABLE IF EXISTS staging_BANK;
 CREATE EXTERNAL TABLE staging_BANK
 USING com.databricks.spark.csv OPTIONS (path ':dataLocation/BANK.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
 CREATE TABLE BANK(BNK_ORG_ID BIGINT NOT NULL,
  BNK_ID  BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  BNK_FULL_NM VARCHAR(50),
  RTNG_NUM VARCHAR(35) NOT NULL,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(150)) USING column OPTIONS(partition_by 'BNK_ORG_ID', buckets '32',key_columns 'CLIENT_ID,BNK_ORG_ID,BNK_ID ' );
  INSERT INTO BANK (SELECT * FROM staging_BANK);

 DROP TABLE IF EXISTS BENEFIT_PACKAGE;
 DROP TABLE IF EXISTS staging_BENEFIT_PACKAGE;
 CREATE EXTERNAL TABLE IF NOT EXISTS  staging_BENEFIT_PACKAGE
   USING com.databricks.spark.csv OPTIONS (path ':dataLocation/BENEFIT_PACKAGE.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
 CREATE TABLE IF NOT EXISTS BENEFIT_PACKAGE(BENE_PKG_ID  BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  BENE_PKG_CD VARCHAR(20) NOT NULL,
  BENE_PKG_TYP_REF_ID BIGINT NOT NULL,
  ORG_ID BIGINT,
  EFF_DT DATE,
  EXPR_DT DATE,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(150)) USING column OPTIONS(partition_by 'BENE_PKG_ID',redundancy '1', buckets '32',key_columns 'CLIENT_ID,BENE_PKG_ID' );
 INSERT INTO BENEFIT_PACKAGE (SELECT * FROM staging_BENEFIT_PACKAGE);

 DROP TABLE IF EXISTS CODE_VALUE;
DROP TABLE IF EXISTS staging_CODE_VALUE;
CREATE EXTERNAL TABLE IF NOT EXISTS  staging_CODE_VALUE
  USING com.databricks.spark.csv OPTIONS (path ':dataLocation/CODE_VALUE.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
CREATE TABLE IF NOT EXISTS CODE_VALUE(CD_VAL_ID BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  CD_VAL VARCHAR(50) NOT NULL,
  CD_TYP_REF_ID BIGINT NOT NULL,
  SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(150),
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP) USING column OPTIONS(partition_by 'CD_VAL_ID', redundancy '1',buckets '32',key_columns 'CLIENT_ID,CD_VAL_ID');
  INSERT INTO CODE_VALUE (SELECT * FROM staging_CODE_VALUE);

DROP TABLE IF EXISTS GROUPS;
DROP TABLE IF EXISTS staging_GROUPS;
CREATE EXTERNAL TABLE IF NOT EXISTS  staging_GROUPS
  USING com.databricks.spark.csv OPTIONS (path ':dataLocation/GROUPS.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
CREATE TABLE IF NOT EXISTS GROUPS(GRP_ID BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  GRP_CD VARCHAR(20),
  GRP_TYP_REF_ID BIGINT NOT NULL,
  ORG_ID BIGINT,
  EFF_DT DATE,
  EXPR_DT DATE,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(150)) USING column OPTIONS(partition_by 'GRP_ID',redundancy '1', buckets '32',key_columns 'CLIENT_ID,GRP_ID' );
 INSERT INTO GROUPS (SELECT * FROM staging_GROUPS);


DROP TABLE IF EXISTS PERSON_EVENT;
DROP TABLE IF EXISTS staging_PERSON_EVENT;
CREATE EXTERNAL TABLE IF NOT EXISTS  staging_PERSON_EVENT
 USING com.databricks.spark.csv OPTIONS (path ':dataLocation/PERSON_EVENT.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
CREATE TABLE IF NOT EXISTS PERSON_EVENT(PRSN_EVNT_ID BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  SRC_TYP_ID BIGINT NOT NULL,
  PRSN_ID BIGINT,
  ATTACH_SRC_ID BIGINT,
  EVNT_TYP_REF_ID BIGINT NOT NULL,
  CLIENT_EVNT_TYP_REF_ID BIGINT,
  EVNT_DESCR VARCHAR(200),
  EFF_DT DATE,
  EXPR_DT DATE,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(200) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(200) NOT NULL) USING column OPTIONS(partition_by 'PRSN_EVNT_ID', redundancy '1',buckets '32',key_columns 'CLIENT_ID,PRSN_EVNT_ID ' );
INSERT INTO PERSON_EVENT (SELECT * FROM staging_PERSON_EVENT);

DROP TABLE IF EXISTS TOPIC_COMMUNICATION;
DROP TABLE IF EXISTS staging_TOPIC_COMMUNICATION;
CREATE EXTERNAL TABLE IF NOT EXISTS  staging_TOPIC_COMMUNICATION
   USING com.databricks.spark.csv OPTIONS (path ':dataLocation/TOPIC_COMMUNICATION.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
CREATE TABLE IF NOT EXISTS TOPIC_COMMUNICATION(CMCN_INQ_ID BIGINT NOT NULL,
  TPC_INQ_ID BIGINT NOT NULL,
  CMCN_ID BIGINT NOT NULL,
  TPC_ID BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  INSRT_USR VARCHAR(50),
  UPD_USR VARCHAR(50),
  INSRT_SERV_REC_TS TIMESTAMP,
  UPD_SERV_REC_TS TIMESTAMP,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(150)) USING column OPTIONS(partition_by 'CMCN_INQ_ID',redundancy '1', buckets '32',key_columns ' CLIENT_ID,CMCN_INQ_ID,TPC_INQ_ID,CMCN_ID,TPC_ID' );
 INSERT INTO TOPIC_COMMUNICATION (SELECT * FROM staging_TOPIC_COMMUNICATION);

DROP TABLE IF EXISTS TOPIC;
DROP TABLE IF EXISTS staging_TOPIC;
CREATE EXTERNAL TABLE IF NOT EXISTS  staging_TOPIC
 USING com.databricks.spark.csv OPTIONS (path ':dataLocation/TOPIC.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
CREATE TABLE IF NOT EXISTS TOPIC(INQ_ID BIGINT NOT NULL,
  TPC_ID BIGINT  NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  CASE_ID VARCHAR(20),
  TPC_STAT_REF_ID BIGINT NOT NULL,
  TPC_SUB_TYP_REF_ID BIGINT NOT NULL,
  TPC_TYP_REF_ID BIGINT NOT NULL,
  PRSN_ID BIGINT,
  DESCR VARCHAR(200),
  DTL_DESCR VARCHAR(200),
  END_TM TIMESTAMP,
  STRT_TM TIMESTAMP,
  WRK_BASKET VARCHAR(50),
  VLD_FRM_DT TIMESTAMP NOT NULL,
  INSRT_USR VARCHAR(50),
  UPD_USR VARCHAR(50),
  INSRT_SERV_REC_TS TIMESTAMP,
  UPD_SERV_REC_TS TIMESTAMP,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(150)) USING column OPTIONS(partition_by 'INQ_ID',buckets '32',redundancy '1',key_columns 'CLIENT_ID,INQ_ID,TPC_ID ' );
 INSERT INTO TOPIC (SELECT * FROM staging_TOPIC);

 DROP TABLE IF EXISTS UM_SERVICE;
 DROP TABLE IF EXISTS staging_UM_SERVICE;
 ----- CREATE TEMPORARY STAGING TABLE TO LOAD CSV FORMATTED DATA -----
 CREATE EXTERNAL TABLE IF NOT EXISTS  staging_UM_SERVICE
     USING com.databricks.spark.csv OPTIONS (path ':dataFilesLocationCol/UM_SERVICE.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
 CREATE TABLE IF NOT EXISTS UM_SERVICE(
 UM_RVW_ID BIGINT NOT NULL,
   UM_SERV_ID BIGINT NOT NULL,
   VER BIGINT NOT NULL,
   CLIENT_ID BIGINT NOT NULL,
   SEQ_NUM INTEGER,
   PRSN_ID BIGINT,
   PRE_AUTH_IND VARCHAR(200),
   REF_IND VARCHAR(200),
   SERV_TYP VARCHAR(200),
   SERV_CAT VARCHAR(200),
   USR_ID VARCHAR(200),
   CRT_DT DATE,
   RCV_DT DATE,
   NXT_RVW_DT DATE,
   AUTH_DT DATE,
   SERV_STAT_CD VARCHAR(200),
   SERV_STAT_DT DATE,
   STAT_SEQ_NUM INTEGER,
   SERV_FRM_DT DATE,
   SERV_TO_DT DATE,
   REQUESTING_PRVD_ID BIGINT,
   SRVC_PRVD_ID BIGINT,
   FACIL_PRVD_ID BIGINT,
   PCP_PRVD_ID BIGINT,
   MED_RLS_IND VARCHAR(200),
   DIAG_CD_ID BIGINT,
   RLTD_DIAG_CD_ID BIGINT,
   SBMT_RLTD_DIAG_CD_ID BIGINT,
   SBMT_DIAG_CD_ID BIGINT,
   RQST_POS_CD_ID BIGINT,
   AUTH_POS_CD_ID BIGINT,
   POS_RSN_REFERENSE_ID BIGINT,
   OUT_OF_AREA_IND VARCHAR(200),
   REFERAL_TYP_REF_ID BIGINT,
   SERV_CD VARCHAR(200),
   SERV_RULE_CD VARCHAR(200),
   SERV_PRC_CD VARCHAR(200),
   SERV_GRP_CD VARCHAR(200),
   PR_CD_ID BIGINT,
   RQST_UNT INTEGER,
   AUTH_UNT INTEGER,
   UNT_DIFFER_RESON_REF_ID BIGINT,
   RQST_CHRG NUMERIC(14,2),
   CONTR_AMT NUMERIC(14,2),
   NEGOTIATE_AMT NUMERIC(14,2),
   NEGOTIATE_PCT NUMERIC(14,2),
   FEE_RSN_REF_ID BIGINT,
   PRC_AMT NUMERIC(14,2),
   PRC_EXPLAIN_ID BIGINT,
   DISALLOW_EXPLAIN_ID BIGINT,
   SERV_DENIAL_RSN_REF_ID BIGINT,
   DENIAL_USR_ID VARCHAR(200),
   AUTH_SVIND VARCHAR(200),
   AUTH_PR_IND VARCHAR(200),
   AUTH_DIAG_IND VARCHAR(200),
   AUTH_PRVD_IND VARCHAR(200),
   CASE_MGMT_SVIND VARCHAR(200),
   CASE_MGMT_PR_IND VARCHAR(200),
   CASE_MGMT_DIAG_IND VARCHAR(200),
   PRD_VIO_IND VARCHAR(200),
   PRD_VIO_RSN_REF_ID BIGINT,
   COMPL_IND VARCHAR(200),
   COMPL_VIO_RSN_REF_ID BIGINT,
   ASST_SURG_IND_1 VARCHAR(200),
   RQST_SEC_OPN_IND VARCHAR(200),
   OBTAINED_SEC_OPN_IND VARCHAR(200),
   ASST_SURG_IND_2 VARCHAR(200),
   INP_USR_SITE_CD VARCHAR(200),
   PCP_IND VARCHAR(200),
   ALWD_AMT NUMERIC(14,2),
   ALWD_UNT INTEGER,
   USE_AMT NUMERIC(14,2),
   USE_UNT INTEGER,
   DENIAL_DT DATE,
   PROVIDER_NETWORK_ID BIGINT,
   PRVD_NTWK_ID VARCHAR(200),
   PRVD_AGREE_ID VARCHAR(200),
   CLIN_EDIT_IND VARCHAR(200),
   CLIN_EDIT_TYP VARCHAR(200),
   CLIN_EDIT_FMT_IND VARCHAR(200),
   CLIN_EDIT_SEQ_NUM INTEGER,
   CALLER_TYP_REF_ID BIGINT,
   ASST_SURG_RSN_REF_ID BIGINT,
   MICROFILM_ID VARCHAR(200),
   PAY_AREA_IND VARCHAR(200),
   SERV_AREA_IND VARCHAR(200),
   RISK_DELEGATED_IND VARCHAR(200),
   RISK_DELEGATED_ENT_ID VARCHAR(200),
   CLM_DELEGATED_IND VARCHAR(200),
   CLM_DELEGATED_ENT_ID VARCHAR(200),
   UM_DELEGATED_IND VARCHAR(200),
   UM_DELEGATED_ENT_ID VARCHAR(200),
   VLD_FRM_DT DATE NOT NULL,
   VLD_TO_DT DATE,
   SRC_SYS_REF_ID VARCHAR(200) NOT NULL,
   SRC_SYS_REC_ID VARCHAR(200),
   OPRN VARCHAR(200)
 ) USING column OPTIONS(partition_by 'UM_RVW_ID',redundancy '1',buckets '32',key_columns 'CLIENT_ID,UM_RVW_ID,UM_SERV_ID ' ) AS (SELECT * FROM staging_UM_SERVICE);
 INSERT INTO UM_SERVICE (SELECT * FROM staging_UM_SERVICE);

 DROP TABLE IF EXISTS BENEFIT_GROUP_NAME;
 DROP TABLE IF EXISTS staging_BENEFIT_GROUP_NAME;
 CREATE EXTERNAL TABLE IF NOT EXISTS  staging_BENEFIT_GROUP_NAME
   USING com.databricks.spark.csv OPTIONS (path ':dataLocation/BENEFIT_GROUP_NAME.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
 CREATE TABLE IF NOT EXISTS BENEFIT_GROUP_NAME(
   GRP_ID BIGINT NOT NULL,
   BENE_GRP_ID BIGINT NOT NULL,
   BENE_GRP_NM_ID BIGINT   NOT NULL,
   VER BIGINT NOT NULL,
   CLIENT_ID BIGINT NOT NULL,
   BENE_GRP_NM VARCHAR(8),
   DESCR VARCHAR(15),
   EFF_DT DATE,
   EXPR_DT DATE,
   VLD_FRM_DT date NOT NULL,
   VLD_TO_DT date,
   SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
   SRC_SYS_REC_ID VARCHAR(15),
   PRIMARY KEY (CLIENT_ID,GRP_ID,BENE_GRP_ID,BENE_GRP_NM_ID)
   )
  USING row OPTIONS(partition_by 'GRP_ID', buckets '32',redundancy '1');
  INSERT INTO BENEFIT_GROUP_NAME SELECT * FROM staging_BENEFIT_GROUP_NAME;

 DROP TABLE IF EXISTS BENEFIT_GROUPS;
 DROP TABLE IF EXISTS staging_BENEFIT_GROUPS;
 CREATE EXTERNAL TABLE IF NOT EXISTS  staging_BENEFIT_GROUPS
 USING com.databricks.spark.csv OPTIONS (path ':dataLocation/BENEFIT_GROUPS.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');
 CREATE TABLE IF NOT EXISTS BENEFIT_GROUPS(
   GRP_ID BIGINT NOT NULL,
   BENE_PKG_ID BIGINT NOT NULL,
   BENE_GRP_ID BIGINT   NOT NULL,
   VER BIGINT NOT NULL,
   CLIENT_ID BIGINT NOT NULL,
   BENE_GRP_CD VARCHAR(5),
   EFF_DT DATE,
   EXPR_DT DATE,
   VLD_FRM_DT date NOT NULL,
   VLD_TO_DT date,
   SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
   SRC_SYS_REC_ID VARCHAR(15),
   PRIMARY KEY (CLIENT_ID,GRP_ID,BENE_PKG_ID,BENE_GRP_ID)
   )
  USING row OPTIONS(partition_by 'GRP_ID', buckets '32',redundancy '1');
  INSERT INTO BENEFIT_GROUPS SELECT * FROM staging_BENEFIT_GROUPS;


