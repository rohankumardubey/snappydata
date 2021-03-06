DROP TABLE IF EXISTS AGREEMENT;
CREATE TABLE AGREEMENT( AGREE_ID BIGINT NOT NULL,
    VER BIGINT NOT NULL,
    CLIENT_ID BIGINT NOT NULL,
    AGREE_CD VARCHAR(200),
    DESCR VARCHAR(200),
    EFF_DT DATE,
    EXPR_DT DATE,
    VLD_FRM_DT TIMESTAMP NOT NULL,
    VLD_TO_DT TIMESTAMP,
    SRC_SYS_REF_ID VARCHAR(200) NOT NULL,
    SRC_SYS_REC_ID VARCHAR(200)) USING column OPTIONS(partition_by 'AGREE_ID', buckets '32',key_columns 'CLIENT_ID,AGREE_ID ',redundancy '1' );
    INSERT into AGREEMENT select id,abs(rand()*1000),abs(rand()*1000),'agree_cd','description','2018-01-01','2019-01-01',from_unixtime(unix_timestamp('2018-01-01 01:00:00')+floor(rand()*31536000)),from_unixtime(unix_timestamp('2019-01-01 01:00:00')+floor(rand()*31536000)),'src_sys_ref_id','src_sys_rec_id' FROM range(50000000);

DROP TABLE IF EXISTS AGREEMENT_ROW;
CREATE TABLE AGREEMENT_ROW( AGREE_ID BIGINT NOT NULL,
    VER BIGINT NOT NULL,
    CLIENT_ID BIGINT NOT NULL,
    AGREE_CD VARCHAR(200),
    DESCR VARCHAR(200),
    EFF_DT DATE,
    EXPR_DT DATE,
    VLD_FRM_DT TIMESTAMP NOT NULL,
    VLD_TO_DT TIMESTAMP,
    SRC_SYS_REF_ID VARCHAR(200) NOT NULL,
    SRC_SYS_REC_ID VARCHAR(200)) USING row OPTIONS(partition_by 'AGREE_ID', buckets '32',redundancy '1' );
    INSERT into AGREEMENT_ROW select id,abs(rand()*1000),abs(rand()*1000),'agree_cd','description','2018-01-01','2019-01-01',from_unixtime(unix_timestamp('2018-01-01 01:00:00')+floor(rand()*31536000)),from_unixtime(unix_timestamp('2019-01-01 01:00:00')+floor(rand()*31536000)),'src_sys_ref_id','src_sys_rec_id' FROM range(500000);

DROP TABLE IF EXISTS AGREEMENT_RR;
CREATE TABLE AGREEMENT_RR( AGREE_ID BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  AGREE_CD VARCHAR(200),
  DESCR VARCHAR(200),
  EFF_DT DATE,
  EXPR_DT DATE,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(200) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(200)) ;
  INSERT into AGREEMENT_RR select id,abs(rand()*1000),abs(rand()*1000),'agree_cd','description','2018-01-01','2019-01-01',from_unixtime(unix_timestamp('2018-01-01 01:00:00')+floor(rand()*31536000)),from_unixtime(unix_timestamp('2019-01-01 01:00:00')+floor(rand()*31536000)),'src_sys_ref_id','src_sys_rec_id' FROM range(10000);

DROP TABLE IF EXISTS BANK;
CREATE TABLE BANK(
  BNK_ORG_ID BIGINT NOT NULL,
  BNK_ID BIGINT NOT NULL,
  VER BIGINT NOT NULL,
  CLIENT_ID BIGINT NOT NULL,
  BNK_FULL_NM VARCHAR(50),
  RTNG_NUM VARCHAR(35) NOT NULL,
  VLD_FRM_DT TIMESTAMP NOT NULL,
  VLD_TO_DT TIMESTAMP,
  SRC_SYS_REF_ID VARCHAR(10) NOT NULL,
  SRC_SYS_REC_ID VARCHAR(150)) USING column OPTIONS(partition_by 'BNK_ORG_ID', buckets '32',key_columns 'CLIENT_ID,BNK_ORG_ID,BNK_ID ',redundancy '1') ;
  INSERT into BANK select id,id,abs(rand()*1000),abs(rand()*1000),'BNK_FULL_NM','RTNG_NUM',from_unixtime(unix_timestamp('2018-01-01 01:00:00')+floor(rand()*31536000)),from_unixtime(unix_timestamp('2019-01-01 01:00:00')+floor(rand()*31536000)),'src_sys_ref_id','src_sys_rec_id' from range(4000000);

DROP TABLE IF EXISTS ORDERS_DETAILS;
CREATE EXTERNAL TABLE staging_orders_details USING com.databricks.spark.csv  OPTIONS (path ':dataLocation/ORDERS_DETAILS.dat', header 'true', inferSchema 'false',nullValue 'NULL', maxCharsPerColumn '4096');
CREATE TABLE ORDERS_DETAILS
             (SINGLE_ORDER_DID BIGINT ,SYS_ORDER_ID VARCHAR(64) ,SYS_ORDER_VER INTEGER ,DATA_SNDG_SYS_NM VARCHAR(128) ,
             SRC_SYS VARCHAR(20) ,SYS_PARENT_ORDER_ID VARCHAR(64) ,SYS_PARENT_ORDER_VER SMALLINT ,PARENT_ORDER_TRD_DATE VARCHAR(20),
             PARENT_ORDER_SYS_NM VARCHAR(128) ,SYS_ALT_ORDER_ID VARCHAR(64) ,TRD_DATE VARCHAR(20),GIVE_UP_BROKER VARCHAR(20) ,
             EVENT_RCV_TS TIMESTAMP ,SYS_ROOT_ORDER_ID VARCHAR(64) ,GLB_ROOT_ORDER_ID VARCHAR(64) ,GLB_ROOT_ORDER_SYS_NM VARCHAR(128) ,
             GLB_ROOT_ORDER_RCV_TS TIMESTAMP ,SYS_ORDER_STAT_CD VARCHAR(20) ,SYS_ORDER_STAT_DESC_TXT VARCHAR(120) ,DW_STAT_CD VARCHAR(20) ,
             EVENT_TS TIMESTAMP,ORDER_OWNER_FIRM_ID VARCHAR(20),RCVD_ORDER_ID VARCHAR(64) ,EVENT_INITIATOR_ID VARCHAR(64),
             TRDR_SYS_LOGON_ID VARCHAR(64),SOLICITED_FG  VARCHAR(1),RCVD_FROM_FIRMID_CD VARCHAR(20),RCV_DESK VARCHAR(20),
             SYS_ACCT_ID_SRC VARCHAR(64) ,CUST_ACCT_MNEMONIC VARCHAR(128),CUST_SLANG VARCHAR(20) ,SYS_ACCT_TYPE VARCHAR(20) ,
             CUST_EXCH_ACCT_ID VARCHAR(64) ,SYS_SECURITY_ALT_ID VARCHAR(64) ,TICKER_SYMBOL VARCHAR(32) ,TICKER_SYMBOL_SUFFIX VARCHAR(20) ,
             PRODUCT_CAT_CD VARCHAR(20) ,SIDE VARCHAR(20) ,LIMIT_PRICE DECIMAL(28, 8),STOP_PRICE DECIMAL(28, 8),ORDER_QTY DECIMAL(28, 4) ,
             TOTAL_EXECUTED_QTY DECIMAL(28, 4) ,AVG_PRICE DECIMAL(28, 8) ,DAY_EXECUTED_QTY DECIMAL(28, 4) ,DAY_AVG_PRICE DECIMAL(28, 8) ,
             REMNG_QTY DECIMAL(28, 4) ,CNCL_QTY DECIMAL(28, 4) ,CNCL_BY_FG  VARCHAR(1) ,EXPIRE_TS TIMESTAMP ,EXEC_INSTR VARCHAR(64) ,
             TIME_IN_FORCE VARCHAR(20) ,RULE80AF  VARCHAR(1) ,DEST_FIRMID_CD VARCHAR(20) ,SENT_TO_CONDUIT VARCHAR(20) ,SENT_TO_MPID VARCHAR(20) ,
             RCV_METHOD_CD VARCHAR(20) ,LIMIT_ORDER_DISP_IND  VARCHAR(1) ,MERGED_ORDER_FG  VARCHAR(1) ,MERGED_TO_ORDER_ID VARCHAR(64) ,
             RCV_DEPT_ID VARCHAR(20) ,ROUTE_METHOD_CD VARCHAR(20) ,LOCATE_ID VARCHAR(256) ,LOCATE_TS TIMESTAMP ,LOCATE_OVERRIDE_REASON VARCHAR(2000) ,
             LOCATE_BROKER VARCHAR(256) ,ORDER_BRCH_SEQ_TXT VARCHAR(20) ,IGNORE_CD VARCHAR(20) ,CLIENT_ORDER_REFID VARCHAR(64) ,
             CLIENT_ORDER_ORIG_REFID VARCHAR(64) ,ORDER_TYPE_CD VARCHAR(20) ,SENT_TO_ORDER_ID VARCHAR(64) ,ASK_PRICE DECIMAL(28, 8) ,
             ASK_QTY DECIMAL(28, 4) ,BID_PRICE DECIMAL(28, 10) ,BID_QTY DECIMAL(28, 4) ,REG_NMS_EXCEP_CD VARCHAR(20) ,REG_NMS_EXCEP_TXT VARCHAR(2000) ,
             REG_NMS_LINK_ID VARCHAR(64) ,REG_NMS_PRINTS  VARCHAR(1) ,REG_NMS_STOP_TIME TIMESTAMP ,SENT_TS TIMESTAMP ,RULE92  VARCHAR(1) ,
             RULE92_OVERRIDE_TXT VARCHAR(2000) ,RULE92_RATIO DECIMAL(25, 10) ,EXMPT_STGY_BEGIN_TIME TIMESTAMP ,EXMPT_STGY_END_TIME TIMESTAMP ,
             EXMPT_STGY_PRICE_INST VARCHAR(2000) ,EXMPT_STGY_QTY DECIMAL(28, 4) ,CAPACITY VARCHAR(20) ,DISCRETION_QTY DECIMAL(28, 4) ,
             DISCRETION_PRICE VARCHAR(64) ,BRCHID_CD VARCHAR(20) ,BASKET_ORDER_ID VARCHAR(64) ,PT_STRTGY_CD VARCHAR(20) ,
             SETL_DATE VARCHAR(20),SETL_TYPE VARCHAR(20) ,SETL_CURR_CD VARCHAR(20) ,SETL_INSTRS VARCHAR(2000) ,COMMENT_TXT VARCHAR(2000) ,
             CHANNEL_NM VARCHAR(128) ,FLOW_CAT VARCHAR(20) ,FLOW_CLASS VARCHAR(20) ,FLOW_TGT VARCHAR(20) ,ORDER_FLOW_ENTRY VARCHAR(20) ,
             ORDER_FLOW_CHANNEL VARCHAR(20) ,ORDER_FLOW_DESK VARCHAR(20) ,FLOW_SUB_CAT VARCHAR(20) ,STRTGY_CD VARCHAR(20) ,RCVD_FROM_VENDOR VARCHAR(20) ,
             RCVD_FROM_CONDUIT VARCHAR(20) ,SLS_PERSON_ID VARCHAR(64) ,SYNTHETIC_FG  VARCHAR(1) ,SYNTHETIC_TYPE VARCHAR(20) ,FXRT DECIMAL(25, 8) ,
             PARENT_CLREFID VARCHAR(64) ,REF_TIME_ID INTEGER ,OPT_CONTRACT_QTY DECIMAL(28, 4) ,OCEAN_PRODUCT_ID BIGINT ,CREATED_BY VARCHAR(64) ,
             CREATED_DATE TIMESTAMP ,FIRM_ACCT_ID BIGINT ,DEST VARCHAR(20) ,CNTRY_CD VARCHAR(20) ,DW_SINGLE_ORDER_CAT VARCHAR(20) ,CLIENT_ACCT_ID BIGINT ,
             EXTERNAL_TRDR_ID VARCHAR(64) ,ANONYMOUS_ORDER_FG  VARCHAR(1) ,SYS_SECURITY_ALT_SRC VARCHAR(20) ,CURR_CD VARCHAR(20) ,
             EVENT_TYPE_CD VARCHAR(20) ,SYS_CLIENT_ACCT_ID VARCHAR(64) ,SYS_FIRM_ACCT_ID VARCHAR(20) ,SYS_TRDR_ID VARCHAR(64) ,DEST_ID INTEGER ,
             OPT_PUT_OR_CALL VARCHAR(20) ,SRC_FEED_REF_CD VARCHAR(64) ,DIGEST_KEY VARCHAR(128) ,EFF_TS TIMESTAMP ,ENTRY_TS TIMESTAMP ,
             OPT_STRIKE_PRICE DECIMAL(28, 8) ,OPT_MATURITY_DATE VARCHAR(20) ,ORDER_RESTR VARCHAR(4) ,SHORT_SELL_EXEMPT_CD VARCHAR(4) ,
             QUOTE_TIME TIMESTAMP ,SLS_CREDIT VARCHAR(20) ,SYS_SECURITY_ID VARCHAR(64) ,SYS_SECURITY_ID_SRC VARCHAR(20) ,SYS_SRC_SYS_ID VARCHAR(20) ,
             SYS_ORDER_ID_UNIQUE_SUFFIX VARCHAR(20) ,DEST_ID_SRC VARCHAR(4) ,GLB_ROOT_SRC_SYS_ID VARCHAR(20) ,GLB_ROOT_ORDER_ID_SUFFIX VARCHAR(64) ,
             SYS_ROOT_ORDER_ID_SUFFIX VARCHAR(20) ,SYS_PARENT_ORDER_ID_SUFFIX VARCHAR(20) ,CREDIT_BREACH_PERCENT DECIMAL(25, 10) ,
             CREDIT_BREACH_OVERRIDE VARCHAR(256) ,INFO_BARRIER_ID VARCHAR(256) ,EXCH_PARTICIPANT_ID VARCHAR(64) ,REJECT_REASON_CD VARCHAR(4) ,
             DIRECTED_DEST VARCHAR(20) ,REG_NMS_LINK_TYPE VARCHAR(20) ,CONVER_RATIO DECIMAL(28, 9) ,STOCK_REF_PRICE DECIMAL(28, 8) ,
             CB_SWAP_ORDER_FG  VARCHAR(1) ,EV DECIMAL(28, 8) ,SYS_DATA_MODIFIED_TS TIMESTAMP ,CMSN_TYPE VARCHAR(20) ,SYS_CREDIT_TRDR_ID VARCHAR(20) ,
             SYS_ENTRY_USER_ID VARCHAR(20) ,OPEN_CLOSE_CD VARCHAR(20) ,AS_OF_TRD_FG  VARCHAR(1) ,HANDLING_INSTR VARCHAR(20) ,SECURITY_DESC VARCHAR(512) ,
             MINIMUM_QTY DECIMAL(21, 6) ,CUST_OR_FIRM VARCHAR(20) ,MAXIMUM_SHOW DECIMAL(21, 6) ,SECURITY_SUB_TYPE VARCHAR(20) ,MULTILEG_RPT_TYPE VARCHAR(4) ,
             ORDER_ACTION_TYPE VARCHAR(4) ,BARRIER_STYLE VARCHAR(4) ,AUTO_IOI_REF_TYPE VARCHAR(4) ,PEG_OFFSET_VAL DECIMAL(10, 2) ,AUTO_IOI_OFFSET DECIMAL(28, 10) ,
             IOI_PRICE DECIMAL(28, 10) ,TGT_PRICE DECIMAL(28, 10) ,IOI_QTY VARCHAR(64) ,IOI_ORDER_QTY DECIMAL(28, 4) ,CMSN VARCHAR(64) ,SYS_LEG_REF_ID VARCHAR(64) ,
             TRADING_TYPE VARCHAR(4) ,EXCH_ORDER_ID VARCHAR(64) ,DEAL_ID VARCHAR(64) ,ORDER_TRD_TYPE VARCHAR(4) ,CXL_REASON VARCHAR(64))
             USING column OPTIONS (partition_by 'SINGLE_ORDER_DID', redundancy '1',buckets '32');

INSERT INTO ORDERS_DETAILS SELECT * FROM staging_orders_details;
DROP TABLE IF EXISTS staging_orders_details;

DROP TABLE IF EXISTS Student;
DROP TABLE IF EXISTS StudentMark;
CREATE TABLE IF NOT EXISTS Student(rollno Int, name String, marks ARRAY<Double>) USING column OPTIONS( buckets '32',redundancy '1');
INSERT INTO Student SELECT 1,'Mohit Shukla', Array(97.8,85.2,63.9,45.2,75.2,96.5);
INSERT INTO Student SELECT 2,'Nalini Gupta',Array(89.3,56.3,89.1,78.4,84.1,99.2);
INSERT INTO Student SELECT 3,'Kareena Kapoor',Array(99.9,25.3,45.8,65.8,77.9,23.1);
INSERT INTO Student SELECT 4,'Salman Khan',Array(99.9,89.2,85.3,90.2,83.9,96.1);
INSERT INTO Student SELECT 5,'Aranav Goswami',Array(90.1,80.1,70.1,60.1,50.1,40.1);
INSERT INTO Student SELECT 6,'Sudhir Chudhari',Array(81.1,81.2,81.3,81.4,81.5,81.6);
INSERT INTO Student SELECT 7,'Anjana Kashyap',Array(71.2,65.0,52.3,89.4,95.1,90.9);
INSERT INTO Student SELECT 8,'Navika Kumar',Array(95.5,75.5,55.5,29.3,27.4,50.9);
INSERT INTO Student SELECT 9,'Atul Singh',Array(40.1,42.3,46.9,47.8,44.4,42.0);
INSERT INTO Student SELECT 10,'Dheeraj Sen',Array(62.1,50.7,52.3,67.9,69.9,66.8);

CREATE VIEW StudentMark AS SELECT rollno,name,explode(marks) AS Marks FROM Student;

DROP TABLE IF EXISTS StudentMarksRecord;
CREATE TABLE IF NOT EXISTS StudentMarksRecord (rollno Integer, name String,Maths MAP<STRING,DOUBLE>,Science MAP<STRING,DOUBLE>, English MAP<STRING,DOUBLE>, Computer MAP<STRING,DOUBLE>, Music MAP<STRING,Double>, History MAP<STRING,DOUBLE>) USING column OPTIONS( buckets '32',redundancy '1');
INSERT INTO StudentMarksRecord SELECT 1,'Mohit Shukla',MAP('maths',97.8),MAP('science',85.2), MAP('english',63.9),MAP('computer',45.2),MAP('music',75.2),MAP('history',96.5);
INSERT INTO StudentMarksRecord SELECT 2,'Nalini Gupta',MAP('maths',89.3),MAP('science',56.3), MAP('english',89.1),MAP('computer',78.4),MAP('music',84.1),MAP('history',99.2);
INSERT INTO StudentMarksRecord SELECT 3,'Kareena Kapoor',MAP('maths',99.9),MAP('science',25.3), MAP('english',45.8),MAP('computer',65.8),MAP('music',77.9),MAP('history',23.1);
INSERT INTO StudentMarksRecord SELECT 4,'Salman Khan',MAP('maths',99.9),MAP('science',89.2), MAP('english',85.3),MAP('computer',90.2),MAP('music',83.9),MAP('history',96.1);
INSERT INTO StudentMarksRecord SELECT 5,'Aranav Goswami',MAP('maths',90.1),MAP('science',80.1), MAP('english',70.1),MAP('computer',60.1),MAP('music',50.1),MAP('history',40.1);
INSERT INTO StudentMarksRecord SELECT 6,'Sudhir Chudhari',MAP('maths',81.1),MAP('science',81.2), MAP('english',81.3),MAP('computer',81.4),MAP('music',81.5),MAP('history',81.6);
INSERT INTO StudentMarksRecord SELECT 7,'Anjana Kashyap',MAP('maths',71.2),MAP('science',65.0), MAP('english',52.3),MAP('computer',89.4),MAP('music',95.1),MAP('history',90.9);
INSERT INTO StudentMarksRecord SELECT 8,'Navika Kumar',MAP('maths',95.5),MAP('science',75.5), MAP('english',55.5),MAP('computer',29.3),MAP('music',27.4),MAP('history',50.9);
INSERT INTO StudentMarksRecord SELECT 9,'Atul Singh',MAP('maths',40.1),MAP('science',42.3), MAP('english',46.9),MAP('computer',47.8),MAP('music',44.4),MAP('history',42.0);
INSERT INTO StudentMarksRecord SELECT 10,'Dheeraj Sen',MAP('maths',62.1),MAP('science',50.7), MAP('english',52.3),MAP('computer',67.9),MAP('music',69.9),MAP('history',66.8);

DROP TABLE IF EXISTS CricketRecord;
CREATE TABLE IF NOT EXISTS CricketRecord(name String,TestRecord STRUCT<batStyle:String,Matches:Long,Runs:Int,Avg:Double>) USING column options(redundancy '1');
INSERT INTO CricketRecord SELECT 'Sachin Tendulkar',STRUCT('Right Hand',200,15921,53.79);
INSERT INTO CricketRecord SELECT 'Saurav Ganguly',STRUCT('Left Hand',113,7212,51.26);
INSERT INTO CricketRecord SELECT 'Rahul Drvaid',STRUCT('Right Hand',164,13288,52.31);
INSERT INTO CricketRecord SELECT 'Yuvraj Singh',STRUCT('Left Hand',40,1900,33.93);
INSERT INTO CricketRecord SELECT 'MahendraSingh Dhoni',STRUCT('Right Hand',90,4876,38.09);
INSERT INTO CricketRecord SELECT 'Kapil Dev',STRUCT('Right Hand',131,5248,31.05);
INSERT INTO CricketRecord SELECT 'Zahir Khan',STRUCT('Right Hand',92,1230,11.94);
INSERT INTO CricketRecord SELECT 'Gautam Gambhir',STRUCT('Left Hand',58,4154,41.96);
INSERT INTO CricketRecord SELECT 'VVS Laxman',STRUCT('Right Hand',134,8781,45.5);
INSERT INTO CricketRecord SELECT 'Virendra Sehwag',STRUCT('Right Hand',104,8586,49.34);
INSERT INTO CricketRecord SELECT 'Sunil Gavaskar',STRUCT('Right Hand',125,10122,51.12);
INSERT INTO CricketRecord SELECT 'Anil Kumble',STRUCT('Right Hand',132,2506,17.65);

DROP TABLE IF EXISTS TwentyTwenty;
CREATE TABLE IF NOT EXISTS TwentyTwenty(name String,LastThreeMatchPerformance ARRAY<Double>,Roll MAP<SMALLINT,STRING>,Profile STRUCT<Matches:Long,Runs:Int,SR:Double,isPlaying:Boolean>) USING column OPTIONS( buckets '32',redundancy '1');
INSERT INTO TwentyTwenty SELECT 'M S Dhoni',ARRAY(37,25,58),MAP(1,'WicketKeeper'),STRUCT(93,1487,127.09,true);
INSERT INTO TwentyTwenty SELECT 'Yuvaraj Singh',ARRAY(68,72,21),MAP(2,'AllRounder'),STRUCT(58,1177,136.38,false);
INSERT INTO TwentyTwenty SELECT 'Viral Kohli',ARRAY(52,102,23),MAP(3,'Batsmen'),STRUCT(65,2167,136.11,true);
INSERT INTO TwentyTwenty SELECT 'Gautam Gambhir',ARRAY(35,48,74),MAP(3,'Batsmen'),STRUCT(37,932,119.02,false);
INSERT INTO TwentyTwenty SELECT 'Rohit Sharma',ARRAY(0,56,44),MAP(3,'Batsmen'),STRUCT(90,2237,138.17,true);
INSERT INTO TwentyTwenty SELECT 'Ravindra Jadeja',ARRAY(15,25,33),MAP(2,'AllRounder'),STRUCT(40,116,93.54,true);
INSERT INTO TwentyTwenty SELECT 'Virendra Sehwag',ARRAY(5,45,39),MAP(3,'Batsmen'),STRUCT(19,394,145.39,false);
INSERT INTO TwentyTwenty SELECT 'Hardik Pandya',ARRAY(27,14,19),MAP(2,'AllRounder'),STRUCT(35,271,153.10,true);
INSERT INTO TwentyTwenty SELECT 'Suresh Raina',ARRAY(31,26,48),MAP(3,'Batsmen'),STRUCT(78,1605,134.87,false);
INSERT INTO TwentyTwenty SELECT 'Harbhajan Singh',ARRAY(23,5,11),MAP(4,'Bowler'),STRUCT(28,108,124.13,false);
INSERT INTO TwentyTwenty SELECT 'Ashish Nehra',ARRAY(2,1,5),MAP(4,'Bowler'),STRUCT(27,28,71.79,false);
INSERT INTO TwentyTwenty SELECT 'Kuldeep Yadav',ARRAY(3,3,0),MAP(4,'Bowler'),STRUCT(17,20,100.0,true);
INSERT INTO TwentyTwenty SELECT 'Parthiv Patel',ARRAY(29,18,9),MAP(1,'WicketKeeper'),STRUCT(2,36,112.50,false);
INSERT INTO TwentyTwenty SELECT 'Ravichandran Ashwin',ARRAY(15,7,12),MAP(4,'Bowler'),STRUCT(46,123,106.95,true);
INSERT INTO TwentyTwenty SELECT 'Irfan Pathan',ARRAY(17,23,18),MAP(2,'AllRounder'),STRUCT(24,172,119.44,false);

DROP TABLE IF EXISTS TwoWheeler;
CREATE TABLE IF NOT EXISTS TwoWheeler (brand String,BikeInfo ARRAY< STRUCT <type:String,cc:Double,noofgears:BigInt,instock:Boolean>>) USING column OPTIONS( buckets '32',redundancy '1');
INSERT INTO TwoWheeler SELECT 'Honda',ARRAY(STRUCT('Street Bike',149.1,5,false));
INSERT INTO TwoWheeler SELECT 'TVS',ARRAY(STRUCT('Scooter',110,0,true));
INSERT INTO TwoWheeler SELECT 'Honda',ARRAY(STRUCT('Scooter',109.19,0,true));
INSERT INTO TwoWheeler SELECT 'Royal Enfield',ARRAY(STRUCT('Cruiser',346.0,5,true));
INSERT INTO TwoWheeler SELECT 'Suzuki', ARRAY(STRUCT('Cruiser',154.9,5,true));
INSERT INTO TwoWheeler SELECT 'Yamaha', ARRAY(STRUCT('Street Bike',149,5,false));
INSERT INTO TwoWheeler SELECT 'Bajaj',ARRAY(STRUCT('Street Bike',220.0,5,true));
INSERT INTO TwoWheeler SELECT 'Kawasaki',ARRAY(STRUCT('Sports Bike',296.0,5,false));
INSERT INTO TwoWheeler SELECT 'Vespa',ARRAY(STRUCT('Scooter',125.0,0,true));
INSERT INTO TwoWheeler SELECT 'Mahindra',ARRAY(STRUCT('Scooter',109.0,0,false));

DROP TABLE IF EXISTS FamousPeople;
CREATE TABLE IF NOT EXISTS FamousPeople(country String,celebrities MAP<String,Array<String>>) USING column OPTIONS( buckets '32',redundancy '1');
INSERT INTO FamousPeople  SELECT 'United States', MAP('Presidents',ARRAY('George Washington','Abraham Lincoln','Thomas Jefferson', 'John F. Kennedy','Franklin D. Roosevelt'));
INSERT INTO FamousPeople  SELECT 'India', MAP('Prime Ministers',ARRAY('Jawaharlal Nehru','Indira Gandhi', 'Lal Bahadur Shastri','Narendra Modi','PV Narsimha Rao'));
INSERT INTO FamousPeople  SELECT 'India', MAP('Actors',ARRAY('Amithab Bachhan','Sanjeev Kumar','Dev Anand', 'Akshay Kumar','Shahrukh Khan','Salman Khan'));
INSERT INTO FamousPeople  SELECT 'United States', MAP('Actors',ARRAY('Brad Pitt','Jim Carry','Bruce Willis', 'Tom Cruise','Michael Douglas','Dwayne Johnson'));
INSERT INTO FamousPeople  SELECT 'India', MAP('Authors',ARRAY('Chetan Bhagat','Jay Vasavada','Amish Tripathi', 'Khushwant Singh','Premchand','Kalidas'));
INSERT INTO FamousPeople  SELECT 'United States', MAP('Authors',ARRAY('Mark Twain','Walt Whitman','J.D. Salinger', 'Emily Dickinson','Willa Cather','William Faulkner'));
CREATE VIEW FamousPeopleView AS  SELECT country, explode(celebrities) FROM FamousPeople;

DROP TABLE IF EXISTS EXEC_DETAILS;
CREATE EXTERNAL TABLE staging_exec_details USING com.databricks.spark.csv
             OPTIONS (path ':dataLocation/EXEC_DETAILS.dat', header 'true', inferSchema 'false', nullValue 'NULL', maxCharsPerColumn '4096');

CREATE TABLE EXEC_DETAILS(
             EXEC_DID BIGINT,SYS_EXEC_VER INTEGER,SYS_EXEC_ID VARCHAR(64),TRD_DATE VARCHAR(20),ALT_EXEC_ID VARCHAR(64),SYS_EXEC_STAT VARCHAR(20),
             DW_EXEC_STAT VARCHAR(20),ORDER_OWNER_FIRM_ID VARCHAR(20),TRDR_SYS_LOGON_ID VARCHAR(64),CONTRA_BROKER_MNEMONIC VARCHAR(20),SIDE VARCHAR(20),
             TICKER_SYMBOL VARCHAR(32),SYS_SECURITY_ALT_ID VARCHAR(64),PRODUCT_CAT_CD VARCHAR(20),LAST_MKT VARCHAR(20),EXECUTED_QTY DECIMAL(18, 4),
             EXEC_PRICE DECIMAL( 18, 8),EXEC_PRICE_CURR_CD VARCHAR(20),EXEC_CAPACITY VARCHAR(20),CLIENT_ACCT_ID BIGINT,FIRM_ACCT_ID BIGINT,
             AVG_PRICE_ACCT_ID BIGINT,OCEAN_ACCT_ID BIGINT,EXEC_CNTRY_CD VARCHAR(20),CMSN VARCHAR(20),COMMENT_TXT VARCHAR(2000),
             ACT_BRCH_SEQ_TXT VARCHAR(20),IGNORE_CD VARCHAR(20),SRC_SYS VARCHAR(20),EXEC_TYPE_CD VARCHAR(20),LIQUIDITY_CD VARCHAR(20),
             ASK_PRICE DECIMAL( 18, 8),ASK_QTY DECIMAL(18, 4),TRD_REPORT_ASOF_DATE VARCHAR(20),BID_PRICE DECIMAL( 18, 8),BID_QTY DECIMAL(18, 4),
             CROSS_ID VARCHAR(64),NYSE_SUBREPORT_TYPE VARCHAR(20),QUOTE_COORDINATOR VARCHAR(20),QUOTE_TIME TIMESTAMP,REG_NMS_EXCEPT_CD VARCHAR(20),
             REG_NMS_EXCEPT_TXT VARCHAR(2000),REG_NMS_LINK_ID VARCHAR(64),REG_NMS_MKT_CENTER_ID VARCHAR(64),REG_NMS_OVERRIDE VARCHAR(20),REG_NMS_PRINTS  VARCHAR(1),
             EXECUTED_BY VARCHAR(20),TICKER_SYMBOL_SUFFIX VARCHAR(20),PREREGNMS_TRD_MOD1  VARCHAR(1),PREREGNMS_TRD_MOD2  VARCHAR(1),PREREGNMS_TRD_MOD3  VARCHAR(1),
             PREREGNMS_TRD_MOD4  VARCHAR(1),NMS_FG  VARCHAR(1),GIVEUP_BROKER VARCHAR(20),CHANNEL_NM VARCHAR(128),ORDER_FLOW_ENTRY VARCHAR(20),FLOW_CAT VARCHAR(20),
             FLOW_CLASS VARCHAR(20),FLOW_TGT VARCHAR(20),ORDER_FLOW_CHANNEL VARCHAR(20),FLOW_SUBCAT VARCHAR(20),SYS_ACCT_ID_SRC VARCHAR(64),STRTGY_CD VARCHAR(20),
             EXECUTING_BROKER_CD VARCHAR(20),LEAF_EXEC_FG  VARCHAR(1),RCVD_EXEC_ID VARCHAR(64),RCVD_EXEC_VER INTEGER,ORDER_FLOW_DESK VARCHAR(20),
             SYS_ROOT_ORDER_ID VARCHAR(64),SYS_ROOT_ORDER_VER INTEGER,GLB_ROOT_ORDER_ID VARCHAR(64),TOTAL_EXECUTED_QTY DECIMAL(18, 4),AVG_PRICE DECIMAL( 18, 8),
             DEST_CD VARCHAR(20),CLIENT_ORDER_REFID VARCHAR(64),CLIENT_ORDER_ORIG_REFID VARCHAR(64),CROSS_EXEC_FG  VARCHAR(1),OCEAN_PRODUCT_ID BIGINT,
             TRDR_ID BIGINT,REF_TIME_ID INTEGER,CREATED_BY VARCHAR(64),CREATED_DATE TIMESTAMP,FIX_EXEC_ID VARCHAR(64),FIX_ORIGINAL_EXEC_ID VARCHAR(64),
             RELATED_MKT_CENTER VARCHAR(20),TRANS_TS TIMESTAMP,SYS_SECURITY_ALT_SRC VARCHAR(20),EVENT_TYPE_CD VARCHAR(20),SYS_CLIENT_ACCT_ID VARCHAR(64),
             SYS_FIRM_ACCT_ID VARCHAR(20),SYS_AVG_PRICE_ACCT_ID VARCHAR(20),SYS_TRDR_ID VARCHAR(64),ACT_BRCH_SEQ VARCHAR(20),SYS_ORDER_ID VARCHAR(64),
             SYS_ORDER_VER INTEGER,SRC_FEED_REF_CD VARCHAR(64),DIGEST_KEY VARCHAR(128),TRUE_LAST_MKT VARCHAR(20),ENTRY_TS TIMESTAMP,OPT_STRIKE_PRICE DECIMAL( 18, 8),
             OPT_MATURITY_DATE VARCHAR(20),EXPIRE_TS TIMESTAMP,OPT_PUT_OR_CALL VARCHAR(20),SYS_ORDER_STAT_CD VARCHAR(20),CONTRA_ACCT VARCHAR(64),CONTRA_ACCT_SRC VARCHAR(20),
             CONTRA_BROKER_SRC VARCHAR(20),SYS_SECURITY_ID VARCHAR(64),SYS_SECURITY_ID_SRC VARCHAR(20),SYS_SRC_SYS_ID VARCHAR(20),SYS_ORDER_ID_UNIQUE_SUFFIX VARCHAR(20),
             DEST VARCHAR(20),DEST_ID_SRC VARCHAR(4),CONVER_RATIO DECIMAL(18, 9),STOCK_REF_PRICE DECIMAL( 18, 8),AS_OF_TRD_FG  VARCHAR(1),MULTILEG_RPT_TYPE VARCHAR(4),
             REG_NMS_LINK_TYPE VARCHAR(20),EXEC_SUB_TYPE VARCHAR(4),CMSN_TYPE VARCHAR(20),QUOTE_CONDITION_IND VARCHAR(20),TRD_THROUGH_FG  VARCHAR(1),
             REGNMS_ORDER_LINK_ID VARCHAR(64),REGNMS_ORDER_LINK_TYPE VARCHAR(20),DK_IND VARCHAR(20),NBBO_QUOTE_TIME VARCHAR(20),GLB_ROOT_SRC_SYS_ID VARCHAR(20),
             TRD_REPORT_TYPE VARCHAR(20),REPORT_TO_EXCH_FG VARCHAR(1),CMPLN_COMMENT VARCHAR(256),DEAL_TYPE VARCHAR(4),EXEC_COMMENTS VARCHAR(256),
             OPTAL_FIELDS VARCHAR(120),SPOT_REF_PRICE VARCHAR(20),DELTA_OVERRIDE VARCHAR(20),UNDERLYING_PRICE VARCHAR(20),PRICE_DELTA VARCHAR(20),
             NORMALIZED_LIQUIDITY_IND VARCHAR(4),USER_AVG_PRICE VARCHAR(20),LAST_EXEC_TS TIMESTAMP,LULD_LOWER_PRICE_BAND VARCHAR(20),LULD_UPPER_PRICE_BAND VARCHAR(20),
             LULD_PRICE_BAND_TS TIMESTAMP,REMNG_QTY DECIMAL(18, 4),ORDER_QTY DECIMAL(18, 4),AMD_TS TIMESTAMP,SETL_CODE VARCHAR(50),SETL_DATE VARCHAR(20),
             CUST_NM VARCHAR(50),EXEC_TYPE VARCHAR(50),TRDR_KEY VARCHAR(50),TRDR_NM VARCHAR(50),FX_RATE VARCHAR(50),CUST_FX_RATE VARCHAR(50),
             PARENT_ORDER_SYS_NM VARCHAR(10),CNC_TYPE VARCHAR(50),FEE_AMT DECIMAL(20, 2),FEE_CCY VARCHAR(10),BRKG_AMT DECIMAL(20, 2),BRKG_CCY VARCHAR(10),
             CLEAR VARCHAR(50),PMT_FIX_DATE VARCHAR(20),FOLLOW_ON_FG  VARCHAR(1),FX_RATE_CCY_TO VARCHAR(10),FX_RATE_CCY_FROM VARCHAR(10),CUST_FX_RATE_CCY_TO VARCHAR(10),
             CUST_FX_RATE_CCY_FROM VARCHAR(10),SYS_GFCID VARCHAR(20),CONTRA_SIDE VARCHAR(20),OPT_CONTRACT_MULTIPLIER DECIMAL(10, 2),PRIOR_REF_PRICE_TS TIMESTAMP,
             SECURITY_SUB_TYPE VARCHAR(20),MSG_DIRECTION VARCHAR(20),LEAF_SYS_EXEC_ID VARCHAR(64),LEAF_SRC_SYS VARCHAR(20),FIX_LAST_MKT VARCHAR(20),
             FIX_CONTRA_BROKER_MNEMONIC VARCHAR(20),RIO_MSG_SRC VARCHAR(64),SNAPSHOT_TS TIMESTAMP,EXTERNAL_TRANS_TS TIMESTAMP,PRICE_CATEGORY VARCHAR(32),
             UNDERLYING_FX_RATE DECIMAL(18, 8),CONVERSION_RATE DECIMAL(18, 8),TRANS_COMMENT VARCHAR(256),AGGRESSOR_FLAG VARCHAR(1))
             USING column OPTIONS (partition_by 'EXEC_DID', redundancy '1', buckets '32');

INSERT INTO EXEC_DETAILS SELECT * FROM staging_exec_details;
DROP TABLE IF EXISTS staging_exec_details;
