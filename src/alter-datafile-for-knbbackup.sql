/**
 * This file is to alter the system and other file's location in control file
 * It is for running the data file from knb backup archive file. Since the user
 * change the files location, the the infomoration need be updated in control file
 * Note: the new location should be correcte and should be modified.
 */
 
 
/**
 *alter database rename file '/oracle01/oradata/knb/system01.dbf' to '/oracle01/oradata/knb/system01.dbf';
 */
 
 alter database rename file '/oracle01/oradata/knb/system02.dbf' to '/oracle02/oradata/knb/system02.dbf';
 
 alter database rename file '/oracle01/oradata/knb/system03.dbf' to '/oracle02/oradata/knb/system03.dbf';
 
 alter database rename file '/oracle01/oradata/knb/drsys01.dbf' to '/oracle03/oradata/knb/drsys01.dbf';
 
 alter database rename file '/oracle01/oradata/knb/indx01.dbf' to '/oracle03/oradata/knb/indx01.dbf';
 
 alter database rename file '/oracle01/oradata/knb/rbs01.dbf' to '/oracle03/oradata/knb/rbs01.dbf';
 
 alter database rename file '/oracle01/oradata/knb/redo01.log' to '/oracle03/oradata/knb/redo01.log';
 
 alter database rename file '/oracle01/oradata/knb/redo02.log' to '/oracle03/oradata/knb/redo02.log';
 
 alter database rename file '/oracle01/oradata/knb/redo03.log' to '/oracle03/oradata/knb/redo03.log';
 
 alter database rename file '/oracle02/oradata/knb/system04.dbf' to '/oracle03/oradata/knb/system04.dbf';
 
 alter database rename file '/oracle01/oradata/knb/temp01.dbf' to '/oracle03/oradata/knb/temp01.dbf';
 
 alter database rename file '/oracle01/oradata/knb/tools01.dbf' to '/oracle03/oradata/knb/tools01.dbf';
 
 alter database rename file '/oracle01/oradata/knb/users01.dbf' to '/oracle03/oradata/knb/users01.dbf';
 