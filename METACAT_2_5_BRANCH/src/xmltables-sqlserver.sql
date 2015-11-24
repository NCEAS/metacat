/*
 * xmltables-sqlserver.sql
 *             : Create or replace tables for storing XML in MS SQL Server
 *
 *      Purpose: creates tables needed for storing XML in MS SQL Server database
 * 
 *      Created: 25 May 2001
 *       Author: Jivka Bojilova
 * Organization: National Center for Ecological Analysis and Synthesis
 *    Copyright: 2000 Regents of the University of California and the
 *               National Center for Ecological Analysis and Synthesis
 *  For Details: http://www.nceas.ucsb.edu/
 *    File Info: '$Id$'
 *
 */

/*
 * Drop all of the objects in proper order
 */
drop table [dbo].[xml_index];
drop table [dbo].[xml_access];
drop table [dbo].[xml_accesssubtree];
drop table [dbo].[xml_revisions];
drop table [dbo].[xml_relation];
drop table [dbo].[xml_documents];
drop table [dbo].[xml_nodes];
drop table [dbo].[xml_replication];
drop table [dbo].[xml_catalog];
drop table [dbo].[accession_number];
drop table [dbo].[harvest_site_schedule];
drop table [dbo].[harvest_log];
drop table [dbo].[harvest_detail_log];
drop table [dbo].[db_version];

/* 
 * ACL -- table to store ACL for XML documents by principals
 */
CREATE TABLE [dbo].[xml_access] (
  [docid]		[varchar] (250) NULL ,
  [accessfileid]	[varchar] (250) NULL ,
  [principal_name]	[varchar] (100) NULL ,
  [permission]		[int] NULL ,
  [perm_type]		[varchar] (50) NULL ,
  [perm_order]		[varchar] (50) NULL ,
  [begin_time]		[datetime] NULL ,
  [end_time]		[datetime] NULL ,
  [ticket_count]	[int] NULL ,
  [subtreeid]  [varchar] (250) NULL ,
  [startnodeid]  [bigint] NULL ,
  [endnodeid]  [bigint] NULL
) ON [PRIMARY]
GO

/* 
 * accesssubtree -- table to store access subtree info 
 */
CREATE TABLE [dbo].[xml_accesssubtree] (
  [docid]		[varchar] (250) NULL ,
  [rev]			[int] NULL , 		
  [controllevel]		[varchar] (250) NULL , 
  [subtreeid]		[varchar] (250) NULL ,
	[startnodeid]  [bigint] NULL ,
  [endnodeid]  [bigint] NULL
)
GO

/* 
 * XML Catalog -- table to store all external sources for XML documents
 */
CREATE TABLE [dbo].[xml_catalog] (
  [catalog_id]		[bigint] IDENTITY (1, 1) NOT NULL ,
  [entry_type]		[varchar] (50) NULL ,
  [source_doctype]	[varchar] (250) NULL ,
  [target_doctype]	[varchar] (250) NULL ,
  [public_id]		[varchar] (250) NULL ,
  [system_id]		[varchar] (512) NULL 
) ON [PRIMARY]
GO

/* 
 * Documents -- table to store XML documents
 */
CREATE TABLE [dbo].[xml_documents] (
  [docid]		[varchar] (250) NOT NULL ,
  [rootnodeid]		[bigint] NULL ,
  [docname]		[varchar] (100) NULL ,
  [doctype]		[varchar] (100) NULL ,
  [user_owner]		[varchar] (100) NULL ,
  [user_updated]	[varchar] (100) NULL ,
  [server_location]	[bigint] NULL ,
  [rev]			[int] NULL ,
  [date_created]	[datetime] NULL ,
  [date_updated]	[datetime] NULL ,
  [public_access]	[bit] NULL ,
  [catalog_id]		[bigint] NULL 
) ON [PRIMARY]
GO

/* 
 * Index of Nodes -- table to store precomputed paths through tree for 
 * quick searching in structured searches
 */
CREATE TABLE [dbo].[xml_index] (
  [nodeid]		[bigint] NOT NULL ,
  [path]		[varchar] (200) NOT NULL ,
  [docid]		[varchar] (250) NULL ,
  [doctype]		[varchar] (100) NULL ,
  [parentnodeid]	[bigint] NULL 
) ON [PRIMARY]
GO

/* 
 * Nodes -- table to store XML Nodes (both elements and attributes)
 */
CREATE TABLE [dbo].[xml_nodes] (
  [nodeid]		[bigint] IDENTITY (1, 1) NOT NULL ,
  [nodeindex]		[int] NULL ,
  [nodetype]		[varchar] (20) NULL ,
  [nodename]		[varchar] (250) NULL ,
  [nodeprefix]	        [varchar] (50) NULL ,
  [nodedata]		[varchar] (4000) NULL ,
  [parentnodeid]	[bigint] NULL ,
  [rootnodeid]		[bigint] NULL ,
  [docid]		[varchar] (250) NULL ,
  [date_created]	[datetime] NULL ,
  [date_updated]	[datetime] NULL, 
  [nodedatanumerical]   [float] NULL,
  [nodedatadate]   [datetime] NULL
) ON [PRIMARY]
GO

/* 
 * Relations -- table to store relations of form <subject,relationship,object>
 */
CREATE TABLE [dbo].[xml_relation] (
  [relationid]		[bigint] IDENTITY (1, 1) NOT NULL ,
  [docid]		[varchar] (250) NULL ,
  [packagetype]		[varchar] (250) NULL ,
  [subject]		[varchar] (250) NOT NULL ,
  [subdoctype]		[varchar] (128) NULL ,
  [relationship]	[varchar] (128) NOT NULL ,
  [object]		[varchar] (250) NOT NULL ,
  [objdoctype]		[varchar] (128) NULL 
) ON [PRIMARY]
GO

/* 
 * Replication -- table to store servers that metacat is replicated to
 */
CREATE TABLE [dbo].[xml_replication] (
  [serverid]		[bigint] IDENTITY (1, 1) NOT NULL ,
  [server]		[varchar] (512) NULL ,
  [last_checked]	[datetime] NULL ,
  [replicate]  [bit] NULL ,
  [datareplicate]  [bit] NULL ,
  [hub]  [bit] NULL
) ON [PRIMARY]
GO
     
set identity_insert xml_replication on
INSERT INTO xml_replication (serverid, server, replicate, datareplicate, hub) VALUES ('1', 'localhost', '0', '0', '0');
set identity_insert xml_replication off 

/* 
 * Revised Documents -- table to store XML documents saved after an UPDATE
 *                    or DELETE
 */
CREATE TABLE [dbo].[xml_revisions] (
  [revisionid]		[bigint] IDENTITY (1, 1) NOT NULL ,
  [docid]		[varchar] (250) NULL ,
  [rootnodeid]		[bigint] NULL ,
  [docname]		[varchar] (100) NULL ,
  [doctype]		[varchar] (100) NULL ,
  [user_owner]		[varchar] (100) NULL ,
  [user_updated]	[varchar] (100) NULL ,
  [server_location]	[bigint] NULL ,
  [rev]			[int] NULL ,
  [date_created]	[datetime] NULL ,
  [date_updated]	[datetime] NULL ,
  [public_access]	[bit] NULL ,
  [catalog_id]		[bigint] NULL 
) ON [PRIMARY]
GO                                

/* 
 * Table used as Unique ID generator for the uniqueid part of Accession#
 */
CREATE TABLE [dbo].[accession_number] (
  [uniqueid]		[int] IDENTITY (1, 1) NOT NULL,
  [site_code]		[varchar] (100),
  [date_created]	[datetime],
  CONSTRAINT [PK_accession_number] PRIMARY KEY CLUSTERED ([uniqueid])
) ON [PRIMARY]
GO

/* 
 * harvest_site_schedule -- table to store harvest sites and schedule info
 */
CREATE TABLE [dbo].[harvest_site_schedule] (
  [site_schedule_id]    [int] IDENTITY (1, 1) NOT NULL ,
  [documentlisturl]		[varchar] (250) NOT NULL ,
  [ldapdn]		        [varchar] (250) NOT NULL ,
  [datenextharvest]		[datetime] NULL ,
  [datelastharvest]	    [datetime] NULL ,
  [updatefrequency]		[int] NULL ,
  [unit]                [varchar] (50) NULL ,
  [contact_email]		[varchar] (50) NULL ,
  [ldappwd]		        [varchar] (20) NOT NULL 
) ON [PRIMARY]
GO

/* 
 * harvest_log -- table to log entries for harvest operations
 */
CREATE TABLE [dbo].[harvest_log] (
  [harvest_log_id]         [int] IDENTITY (1, 1) NOT NULL ,
  [harvest_date]		   [datetime] NOT NULL ,
  [status]		           [int] NOT NULL ,
  [message]		           [varchar] (1000) NULL ,
  [harvest_operation_code] [varchar] (30) NOT NULL ,
  [site_schedule_id]       [int] NOT NULL 
) ON [PRIMARY]
GO

/* 
 * harvest_detail_log -- table to log detailed info about documents that
 *                       generated errors during the harvest
 */
CREATE TABLE [dbo].[harvest_detail_log] (
  [detail_log_id]   [int] IDENTITY (1, 1) NOT NULL ,
  [harvest_log_id]	[int] NOT NULL ,
  [scope]		    [varchar] (50) NOT NULL ,
  [identifier]		[bigint] NOT NULL ,
  [revision]	    [bigint] NOT NULL ,
  [document_url]	[varchar] (255) NOT NULL ,
  [error_message]   [varchar] (1000) NOT NULL ,
  [document_type]   [varchar] (100) NOT NULL 
) ON [PRIMARY]
GO

/* 
 * db_version -- table to store the version history of this database
 */
CREATE TABLE [dbo].[db_version] (
  [db_version_id]	[bigint] IDENTITY (1, 1) NOT NULL ,
  [version]		    [varchar] (250) NOT NULL ,
  [status]		    [int] NOT NULL ,
  [date_created]	[datetime] NULL
) ON [PRIMARY]
GO

/* 
 * Constraints and indexes
 */
ALTER TABLE [dbo].[xml_catalog] WITH NOCHECK ADD 
	CONSTRAINT [PK_xml_catalog] PRIMARY KEY  CLUSTERED 
	(
		[catalog_id]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_documents] WITH NOCHECK ADD 
	CONSTRAINT [PK_xml_documents] PRIMARY KEY  CLUSTERED 
	(
		[docid]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_index] WITH NOCHECK ADD 
	CONSTRAINT [PK_xml_index] PRIMARY KEY  CLUSTERED 
	(
		[nodeid],
		[path]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_nodes] WITH NOCHECK ADD 
	CONSTRAINT [PK_xml_nodes] PRIMARY KEY  CLUSTERED 
	(
		[nodeid]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_relation] WITH NOCHECK ADD 
	CONSTRAINT [PK_xml_relation] PRIMARY KEY  CLUSTERED 
	(
		[relationid]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_replication] WITH NOCHECK ADD 
	CONSTRAINT [PK_xml_replication] PRIMARY KEY  CLUSTERED 
	(
		[serverid]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_revisions] WITH NOCHECK ADD 
	CONSTRAINT [PK_xml_revisions] PRIMARY KEY  CLUSTERED 
	(
		[revisionid]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[harvest_site_schedule] WITH NOCHECK ADD 
	CONSTRAINT [PK_harvest_site_schedule] PRIMARY KEY  CLUSTERED 
	(
		[site_schedule_id]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[harvest_log] WITH NOCHECK ADD 
	CONSTRAINT [PK_harvest_log] PRIMARY KEY  CLUSTERED 
	(
		[harvest_log_id]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[harvest_detail_log] WITH NOCHECK ADD 
	CONSTRAINT [PK_harvest_detail_log] PRIMARY KEY  CLUSTERED 
	(
		[detail_log_id]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[db_version] WITH NOCHECK ADD 
	CONSTRAINT [PK_db_version] PRIMARY KEY  CLUSTERED 
	(
		[db_version_id]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_access] WITH NOCHECK ADD 
	CONSTRAINT [CK_xml_access] CHECK ([begin_time] < [end_time])
GO

ALTER TABLE [dbo].[xml_catalog] WITH NOCHECK ADD 
	CONSTRAINT [IX_xml_catalog] UNIQUE  NONCLUSTERED 
	(
		[entry_type],
		[source_doctype],
		[target_doctype],
		[public_id]
	)  ON [PRIMARY] 
GO

ALTER TABLE [dbo].[xml_documents] WITH NOCHECK ADD 
	CONSTRAINT [DF_xml_documents_rev] DEFAULT (1) FOR [rev]
GO

ALTER TABLE [dbo].[xml_accesssubtree] WITH NOCHECK ADD 
	CONSTRAINT [DF_xml_accesssubtree_rev] DEFAULT (1) FOR [rev]
GO

ALTER TABLE [dbo].[xml_relation] WITH NOCHECK ADD 
	CONSTRAINT [IX_xml_relation] UNIQUE  NONCLUSTERED 
	(
		[docid],
    [subject],
		[relationship],
		[object]
	)  ON [PRIMARY] 
GO

 CREATE  INDEX [IX_xml_documents] ON [dbo].[xml_documents]([docid], [doctype])
                                                                    ON [PRIMARY]
GO

 CREATE  INDEX [IX_xml_index] ON [dbo].[xml_index]([path]) ON [PRIMARY]
GO

 CREATE  INDEX [IX1_xml_nodes] ON [dbo].[xml_nodes]([rootnodeid]) ON [PRIMARY]
GO

 CREATE  INDEX [IX2_xml_nodes] ON [dbo].[xml_nodes]([parentnodeid]) ON [PRIMARY]
GO

 CREATE  INDEX [IX3_xml_nodes] ON [dbo].[xml_nodes]([nodename]) ON [PRIMARY]
GO

ALTER TABLE [dbo].[xml_accesssubtree] ADD 
	CONSTRAINT [FK_xml_accesssubtree_xml_documents] FOREIGN KEY
  (
		[docid]
	) REFERENCES [dbo].[xml_documents] (
		[docid]
	)
GO

ALTER TABLE [dbo].[xml_access] ADD 
	CONSTRAINT [FK_xml_access_xml_documents] FOREIGN KEY 
	(
		[accessfileid]
	) REFERENCES [dbo].[xml_documents] (
		[docid]
	)
GO

ALTER TABLE [dbo].[xml_documents] ADD 
	CONSTRAINT [FK_xml_documents_xml_catalog] FOREIGN KEY 
	(
		[catalog_id]
	) REFERENCES [dbo].[xml_catalog] (
		[catalog_id]
	),
	CONSTRAINT [FK_xml_documents_xml_nodes] FOREIGN KEY 
	(
		[rootnodeid]
	) REFERENCES [dbo].[xml_nodes] (
		[nodeid]
	),
	CONSTRAINT [FK_xml_documents_xml_replication] FOREIGN KEY 
	(
		[server_location]
	) REFERENCES [dbo].[xml_replication] (
		[serverid]
	)
GO

ALTER TABLE [dbo].[xml_index] ADD 
	CONSTRAINT [FK_xml_index_xml_documents] FOREIGN KEY 
	(
		[docid]
	) REFERENCES [dbo].[xml_documents] (
		[docid]
	),
	CONSTRAINT [FK_xml_index_xml_nodes] FOREIGN KEY 
	(
		[nodeid]
	) REFERENCES [dbo].[xml_nodes] (
		[nodeid]
	)
GO

ALTER TABLE [dbo].[xml_nodes] ADD 
	CONSTRAINT [FK_xml_nodes_parentnodeid] FOREIGN KEY 
	(
		[parentnodeid]
	) REFERENCES [dbo].[xml_nodes] (
		[nodeid]
	),
	CONSTRAINT [FK_xml_nodes_rootnodeid] FOREIGN KEY 
	(
		[rootnodeid]
	) REFERENCES [dbo].[xml_nodes] (
		[nodeid]
	)
GO

ALTER TABLE [dbo].[xml_relation] ADD 
	CONSTRAINT [FK_xml_relation_xml_documents] FOREIGN KEY 
	(
		[docid]
	) REFERENCES [dbo].[xml_documents] (
		[docid]
	)
GO

ALTER TABLE [dbo].[xml_revisions] ADD 
	CONSTRAINT [FK_xml_revisions_xml_catalog] FOREIGN KEY 
	(
		[catalog_id]
	) REFERENCES [dbo].[xml_catalog] (
		[catalog_id]
	),
	CONSTRAINT [FK_xml_revisions_xml_nodes] FOREIGN KEY 
	(
		[rootnodeid]
	) REFERENCES [dbo].[xml_nodes] (
		[nodeid]
	),
	CONSTRAINT [FK_xml_revisions_xml_replication] FOREIGN KEY 
	(
		[server_location]
	) REFERENCES [dbo].[xml_replication] (
		[serverid]
	)
GO

ALTER TABLE [dbo].[harvest_detail_log] ADD 
	CONSTRAINT [FK_harvest_detail_log_harvest_log] FOREIGN KEY 
	(
		[harvest_log_id]
	) REFERENCES [dbo].[harvest_log] (
		[harvest_log_id]
	)
GO

