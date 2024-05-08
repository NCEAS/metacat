Storage Subsystem
=================

Use Cases
---------

-  **UC01: {Repository operators, researchers} need to upload large, or
   many files**

   -  **Priority: 2** (blocked by UC04)
   -  High volume network transfer, e.g., over Globus or other non-http
      protocols)
   -  Bootstrap new MN install (e.g., ADC) with, say, 5TB of data over
      many files w/o paying the network transfer penalty associated with
      MNStorage.create
   -  MN operators (e.g., ADC) loading large submissions. Currently
      might take hours/days. We might call this “out of band data
      loading”
   -  Have alternative options for users that might have slow internet
      speeds (ability to resume where they might have left off)
   -  Arctic Data Center, ESS-DIVE, PDG, DataONE

-  **UC02: {Repository operators, researchers} want to create/update
   file system based data uploads**

   -  Upload a single file (data or metadata) to the file system
   -  Upload a whole package with all of its metadata and data files in
      a package hierarchy

      -  triggers creation of system metadata?

   -  “zip-based” upload is start of support for create/update
      transactions and rollback
   -  ESS-DIVE, rdataone, …

-  **UC03: {Researchers} needs a hierarchical file system read-only view
   of packages**

   -  e.g., run analysis against files in package hierarchy
   -  view should be read-only
   -  need to enforce access control rules (e.g., only show readable
      files and packages)

-  **UC04: {MN System tools} need direct access to metadata/data**

   -  **Priority: 1** (blocks others)
   -  priviledged tools that can access all data
   -  only available to ‘admin’ users/tools
   -  MetaDIG quality engine needs direct, efficient access to
      metadata/data

      -  would enable metadata/data congruence checks
      -  would enable advanced data checks
      -  Need access to files from distributed clusters like Kubernetes

   -  DataONE LOD service needs read-only access to metadata (possibly
      data). Currently gets it all over HTTP
   -  Efficient data previews in the UI or other client tools
   -  ESS-DIVE file checks of data (e.g., for virus checking)
   -  Indexer needs access to on-disk metadata files and resource maps
      and system-metadata

      -  Currently give file path to bytes for science metadata and
         resource maps,
      -  system metadata comes from HZ, needs to be accessible from the
         filesystem too
      -  Need access to files from distributed clusters like Kubernetes

   -  System metadata needs to be stored on the filesystem
   -  Service to create checksums for all data objects (i.e. Carl
      Bottinger)

      -  a suite of checksums could be calculated and published to a
         registry

   -  Metrics Service access to the metadata to parse citations,…
   -  ADC, ESS_DIVE, DataONE,

-  **UC05: {Researchers} create and trigger data quality assessment
   workflows**

   -  for Arctic Data Center, with user contributed data checking
      workflows

      -  requires access to files on disk, arranged in hierarchical
         packages as the user “sees” it

-  **UC06a: Downloading large packages via BagIt over HTTP**

   -  Need ability to download large packages in the face of network
      errors - HTTP Byte-range support
   -  Fix our Bagit download implementation
   -  The ability to download the files based on how on the hierarchical
      file system (eg. I want to download all the files from the same
      folder)

-  **UC06: Downloading large packages via alternate protocols**

   -  **Priority: 3**
   -  Bulk download is too difficult currently for people and I can
      imagine us being able to set up a permanent or temporary FTP or
      something to give durable access
   -  Subcase 1: download all of Kate Stafford’s files

      -  Requires a large bagit beyond our current capabilities

   -  Subcase 2: mark and download a subset of the files in a package
      hierarchy
   -  Subcase 3: Support OPeNDAP/THREDDS access
   -  Globus support
   -  Being able to download a specific group of data files (eg. all the
      data related to a sample)

-  **UC07: {Researchers} want to remotely mount a filesystem view of
   their {package, collection, repository, DataONE}**

   -  Same as ONEDrive use cases from DataONE Phase I

-  **UC08: {Researchers} want to upload their data packages via Google
   Drive/Box/Dropbox/OneDrive/GitHub**

   -  Fill in metadata somewhere, tag a release, get’s uploaded
      automatically
   -  science metadata gets added on/layered on top of files obtained
      from shared drives, etc.

-  **UC09: {Contributors} want to do batch updates to {large} number of
   existing files**

   -  Need a batch API for things like access control updates e.g. send
      a single API request for various metadata about a collection of
      objects (rather than sending one call for each type of metadata
      for each object)
   -  https://hpad.dataone.org/fJtGygXLRH-pUGG4XnPDWw#

-  **UC10: {Contributors} want to be able to upload and view files in
   folder structures**

   -  For both small and large packages
   -  Involves how we rearchitect packages as large collections of files

-  ESS-DIVE use cases

   -  some files are too large to upload to DataONE

      -  links are made to the data

   -  essdive defined access types

      -  tier 1

         -  in metacat, replicated

      -  tier 2

         -  not in metacat

      -  tier 3

         -  offline, private
         -  requires request, manual access

   -  direct access to data from Jupyter notebook
   -  user creates a list of files to download, creating a collection
      like a shopping cart, then requests download

      -  maybe a BagIt file is created

   -  on upload

      -  check that files are in required format

Requirements (Capabilities)
---------------------------

-  ability to upload very large files, efficiently (current Metacat
   limit is 5MB)
-  ability to upload many files (thousands), efficiently
-  provide high volume, non-HTTP access to Metacat data store
-  provide public or authenticated access depending on object
-  provide a hierarchical view of Metacat data store
-  files ingested/uploaded are integrated into DataONE as if they were
   uploaded via DataONE REST API

   -  system metadata created
   -  objects indexed in Solr

-  access via the storage API should have better performance than the
   DataONE REST API
-  for clients (e.g. MetaDIG enginee)

   -  read access needed for all DataONE metadata
   -  read access needed for potentially all DataONE data objects
   -  more effiecient than downloading metadata for each run

-  read only access to hierarchical view of any data package
-  read only access to all DataONE system metadata
-  Batch updates in Metacat
   (https://hpad.dataone.org/fJtGygXLRH-pUGG4XnPDWw#)


Design diagrams
---------------

Architecture
~~~~~~~~~~~~

.. figure:: images/mc-overview.png
   :align: center

   Figure 1. Metacat components overview, highlighting index task flow with dashed arrows.

..
  This block defines the components diagram referenced above.
  @startuml images/mc-overview.png
  top to bottom direction
  !theme superhero-outline
  !include <logos/solr>
  skinparam actorStyle awesome

  together {
    :Alice:
    :Bob:
    :Chandra:
  }

  frame "Ceph Cluster" as cluster {
    component CephFS
    component "hosts" {
      database "ceph-host-1"
      database "ceph-host-2"
      database "ceph-host-3"
      database "ceph-host-n"
    }
    CephFS-hosts
  }

  frame DataONEDrive as d1d {
    [Globus]
    [WebDAV]
    d1d-u-cluster
  }

  frame "Postgres deployment" as pg {
    database metacat as mcdb {
    }
  }

  frame "Metacat" {
    interface "DataONE API" as D1
    :Alice: -d-> D1
    :Bob: -d-> D1
    :Chandra: -d-> D1
    [MetacatHandler]--D1
    [Task Generator] <.. [MetacatHandler]
    [Search] <-- [MetacatHandler]
    [Auth] <-- [MetacatHandler]
    [EventLog] <-- [MetacatHandler]
    [Replicate] <-- [MetacatHandler]
    frame "Storage Subsystem" as Storage {

      frame "Storage Adapters" as store {
        component ceph
        component S3
        component LocalFS
      }
      rectangle DataObject as do {
              () Read as R
              () Write as W
      }
      do--R
      do--W
      do --> ceph
      do <-- ceph
    }
    W <-- [MetacatHandler]
    R <-- [MetacatHandler]
    do--pg
  }

  frame "dataone-index" as indexer {
    frame "RabbitMQ deployment" {
      interface basicPublish
      queue PriorityQueue as pqueue
      [Monitor]
      basicPublish .> pqueue
    }

    node "MC Index" {
      [Index Worker 1] as iw1
      [Index Worker 2] as iw2
      [Index Worker 3] as iw3
      pqueue ..> iw1
      pqueue ..> iw2
      pqueue ..> iw3
    }

    frame "SOLR deployment" as solr {
      database "<$solr>" as s {
        folder "Core" {
          [Index Schema]
        }
      }
    }
  }

  [Task Generator] ..> basicPublish : add
  iw1 --> [Index Schema] : update
  iw2 --> [Index Schema] : update
  iw3 --> [Index Schema] : update
  ceph <-- CephFS : read
  ceph --> CephFS : write
  iw1 ..> CephFS : read
  iw2 ..> CephFS : read
  iw3 ..> CephFS : read
  Search --> solr

  @enduml

.. raw:: latex

  \newpage

.. raw:: pdf

  PageBreak


Design choices include:

- Storage interface
  
    - Ceph FS
        - see https://docs.ceph.com/en/latest/cephfs/
    - Ceph Object Gateway
        - S3 API (see https://docs.ceph.com/en/latest/radosgw/s3/)
        - Swift API 
  
- Data and metadata access layer
  
    - What level should we plan for access by:
        - System processes
        - User access processes

Physical File Layout (HashStore)
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

For physical file storage and layout, our goal is to provide a consistent
directory structure that will be robust against naming issues such as
illegal characters, and that allows us to access both system metadata and
the file contents knowing only the PID for an object. This approach focuses
on using a content hash identifier for naming data and metadata objects in
their respective folders, rather than hashes generated from an authority-based
identifiers such as a PID or SID, along with reference files to catalogue data.

 **Why not use hash identifiers based on a PID?**

 During the initial development phase, HashStore stored objects based on
 a unique identifier generated from hashing a given PID (ex. storing both data
 and metadata in its respective directories using this hash as the permanent
 address). This lessened the potential issues with file locking in operating
 systems by allowing greater control when synchronizing requests to store
 objects.

 This change also meant that HashStore would potentially store duplicate
 objects, which was an accepted tradeoff. However, the usage of multipart
 http requests in Metacat - which transports the pieces of data that users
 upload in "body parts" when submitting datasets, meant that the existing
 storage operation would become a non-atomic process if HashStore relied
 on a PID value.

 In multipart http requests, the order in which data and metadata is received by
 Metacat cannot be guaranteed. Since the PID resides with the metadata "body part",
 a PID may not always be immediately available for Metacat to call store object
 with. So if a data "body part" were to arrive first, it would need to be stored
 and held as a temporary object until the metadata "body part" is received.

 In the above scenario where a temporary object awaits its final location, we
 would have also already calculated the default list of hashes (content identifiers).
 So storing the data object with its content identifier as its permanent address
 (and in its own directory) enables this storage process to be atomic - an object
 is either stored or it isn't. There is no waiting period for a data object to
 be completely stored, or potentially lost in limbo, and objects are only stored once.

 If we could guarantee the order of uploads to Metacat, then using the hash identifier
 generated from a PID would be suitable. We have discussed this potential change
 of forcing the metadata "body part" to arrive first during the upload process, but
 it would require extensive changes to processes which we do not have the resources or
 time to undertake (nor the desire to force upon existing and new users).

 - To learn more about the initial design, please see: storage-subsystem-cid-file-layout.rst
 - To learn more about http multipart requests, please see: https://www.w3.org/Protocols/rfc1341/7_2_Multipart.html

**Raw File Storage**

The raw bytes of each object (data, metadata, or resource map) are first written
into a temporary file while its content identifiers are calculated (using a hashing
algorithm such that each unique set of bytes produces a unique checksum value).
That checksum value is then used to name the file. In this way, even when the same
file is uploaded multiple times, it will only be stored once in the filesystem.

If an object already exists, no exception should be thrown and its respective object
info/metadata is returned upon successful storage.

 **Note:** While metadata objects share the same flow, only one of each metadata
 type (ex. sysmeta, annotation, etc.) can exist at any time. Multiple calls to store
 metadata will all be executed in chronological order, and replace any existing files
 of its respective metadata type (formatId).

**Checksum algorithm and encoding**

We have multiple hash algorithms to choose from, and each has multiple ways of
encoding the binary hash into a string representation. We will choose the
simplest, most common configuration which is to use a `SHA-256` hash
algorithm, with the binary digest converted to a string value using `base64`
encoding. That makes each hash value 64 characters long (representing
the 256 bit binary value). For example, here is a base64-encoded SHA-256 value:

   4d198171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c

While we chose this common combination, we could also have chosen other hash
algorithms (e.g., SHA-1, SHA3-256, blake2b-512) and alternate string encodings
(e.g., base58, Multihash (https://multiformats.io/multihash/)). Multihash may be
a valuable approach in to future-proof the storage system, because it enables use
of multiple checksum algorithms.

**Folder layout**

To reduce the number of files in a given directory, we use the first several
characters of the hash to create a directory hierarchy and divide the files up to
make the tree simpler to explore and less likely to exceed operating system
limits on files. We store all objects in an `objects` directory, with 3
levels of 'depth' and a 'width' of 2 digits (https://github.com/DataONEorg/hashstore/issues/3).
Because each digit in the hash can contain 16 values, the directory structure can
contain 16,777,216 subdirectories (256^3). An example file layout for three objects would be::

   /var/metacat/hashstore
   └── objects
       ├── 4d
       │   └── 19
       │       └── 8171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c
       ├── 94
       │   └── f9
       │       └── b6c88f1f458e410c30c351c6384ea42ac1b5ee1f8430d3e365e43b78a38a
       └── 44
           └── 73
               └── 516a592209cbcd3a7ba4edeebbdb374ee8e4a49d19896fafb8f278dc25fa

   // Additional Context for Examples (Test Data Items):
   // - doi:10.18739/A2901ZH2M
   // -- SHA-256 Content Hash: 4d198171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c
   // - jtao.1700.1
   // -- SHA-256 Content Hash: 94f9b6c88f1f458e410c30c351c6384ea42ac1b5ee1f8430d3e365e43b78a38a
   // - urn:uuid:1b35d0a5-b17a-423b-a2ed-de2b18dc367a
   // -- SHA-256 Content Hash: 4473516a592209cbcd3a7ba4edeebbdb374ee8e4a49d19896fafb8f278dc25fa

Note how the full hash value is obtained by appending the directory names with
the file name

 e.g., `4d198171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c` for the first object

**Metadata Storage**

A mechanism is needed to store metadata about the object, including its persistent
identifier (PID) and system metadata for the object. While we currently only have
a need to access system metadata for each object, in the future we envision
potentially including other metadata files that can be used for describing
individual data objects. This might include package relationships and other
annotations that we wish to include for each data file.

So, in addition to data objects, the system supports storage for multiple metadata
documents that are associated with a given PID, and the creation of reference files
that facilitate the relationship that exists with a given PID. All metadata documents
will be stored in the `/metadata` directory. Each `PID` will have its own
subdirectory based on the hash of its respective PID. In this directory, each
metadata document's file name is calculated using the SHA-256 hash of the `PID` + `formatId`.

For example, one can find the directory that contains all the related metadata
documents of the given PID `jtao.1700.1` by calculating the SHA-256 of that PID using::

    $ echo -n "jtao.1700.1" | shasum -a 256
    a8241925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf

    So, the system metadata file (sysmeta), along with all other metadata related to the PID,
    would be stored in the folder:

    `.../metadata/a8/24/1925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf/`

    /var/metacat/hashstore
    ├── objects
    |   └── ...
    └── metadata
        └── a8
            └── 24
                └── 1925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf
                    └── sha256("jtao.1700.1"+"http://ns.dataone.org/service/types/v2.0") // sysmeta namespace
                    └── sha256(pid+formatId_annotations)


Extending our diagram from earlier & above, we now see the three hashes that represent
data files, along with three that represent the metadata directory for a given PID,
and the relevant metadata files - each named with the hash of the `PID` + `formatId`
they describe::

   /var/metacat/hashstore
   ├── objects
   |   ├── 4d
   |   │   └── 19
   |   │       └── 8171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c
   |   ├── 94
   |   │   └── f9
   |   │       └── b6c88f1f458e410c30c351c6384ea42ac1b5ee1f8430d3e365e43b78a38a
   |   └── 44
   |       └── 73
   |           └── 516a592209cbcd3a7ba4edeebbdb374ee8e4a49d19896fafb8f278dc25fa
   └── metadata
       ├── 0d
       │   └── 55
       │       └── 555ed77052d7e166017f779cbc193357c3a5006ee8b8457230bcf7abcef65e
       |           └── sha256(pid+formatId_sysmeta)
       |           └── sha256(pid+formatId_annotations)
       ├── a8
       │   └── 24
       │       └── 1925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf
       |           └── sha256(pid+formatId_sysmeta)
       |           └── sha256(pid+formatId_annotations)
       └── 7f
           └── 5c
               └── c18f0b04e812a3b4c8f686ce34e6fec558804bf61e54b176742a7f6368d6
                   └── sha256(pid+formatId_sysmeta)
                   └── sha256(pid+formatId_annotations)

 **Metadata Format & Reference File Process**

 Initially, system metadata files were proposed to be stored in the format
 of a delimited file with a header and body section. The header contains
 the 64-character hash (content identifier) of the data file described by this
 sysmtem metadata (sysmeta), followed by a space, then the `formatId` of
 the metadata format for the metadata in the file, and then a NULL (`\x00`).
 This header is then followed by the content of the metadata document in UTF-8
 encoding.

 This metadata file's permanent address is calculated by using the SHA-256
 hash of the persistent identifier (PID) of the object, and stored in a `sysmeta`
 directory parallel to the one described above for objects, and structured analogously.
 So given just the `sysmeta` directory, we could reconstruct an entire member node's
 data and metadata content.

 However, to reduce complexity in HashStore, we switched to a system that uses
 PID-based hash identifiers. In this proposed system:

 - Objects are stored using the hash of the given PID in the `/objects` directory.
 - Metadata documents are stored using the hash of the given PID in the `/metadata`
   directory, with each metadata document's permanent address formed by the hash
   of the `PID` + `formatId`.

 This proposed change made the initial sysmeta delimiter format redundant.
 The `formatId`, which is required for identifying and storing a metadata
 document, was incorporated directly into the metadata storage process,
 eliminating the need for its inclusion in the delimiter format. Additionally,
 a PID's respective object could be found directly by hashing the PID to get
 its location in the `/objects` directory. Now, only the body portion
 (metadata content) was necessary in a stored metadata document.

 However, we had to reverse course on this approach due to the way uploads are
 handled in Metacat. While we kept the change to handle the storage of multiple
 metadata documents for a given pid, this reversal and transition back to a
 content identifier-based approach necessitated the reintroduction
 of a way to manage the relationships of a given PID and CIDs in HashStore.

 Instead of returning back to the original format, reference files were introduced
 to contain the header content. This shift eliminates the need to fully parse a
 system metadata document to retrieve a metadata document. Additionally, by making
 the relationship more transparent in this way, any user (even those without
 comprehensive knowledge of HashStore), can easily navigate and find the connections
 for a given PID directly through the file layout of HashStore.

 To learn about the project that spurred this change, please see: https://github.com/mbjones/dip-noodling

**Reference Files (a.k.a. Tags)**

To manage the relationship between objects and metadata, reference files are created
in a separate `refs` directory, parallel to `objects` and `metadata`. This `refs`
directory includes a subdirectory for objects (`/refs/cid`, for content identifiers)
and a subdirectory for metadata (`/refs/pid`, for persistent identifiers).

 HashStore Reference Files Implementation

 1. **Cid Reference File**

    A cid reference file for each object is created during the first storage
    operation for the object. This file lists persistent identifiers (pids)
    associated with the object, each delimited by a newline (`\n`). The cid reference
    file is updated when a duplicate object is stored or an object is deleted.
    An object cannot be deleted if its cid reference file is present, and this file
    (and the associated object) can only be deleted when there are no more references
    to the object.

    The permanent address of an object’s cid reference file is calculated using
    its content identifier, and follows the HashStore folder layout structure.

 2. **Pid Reference File**

    A pid reference file is created during all object storage operations given the
    PID is unique. This reference file contains a single line representing
    the content identifier of the object. This file is never updated, as a PID can
    only reference one CID. When an object is deleted (using a given PID),
    the pid reference file is deleted, and the reference is removed from the cid
    reference file.

    The permanent address of a metadata’s pid reference file is determined by
    using the SHA-256 hash of the PID, and follows the HashStore folder layout structure.

So with sufficient privileges, an administrator or third party can manually traverse
the file layout system (HashStore) if desired to retrieve an object with a given PID without
interacting with Metacat. Below, is the full proposed HashStore file layout diagram::

   /var/metacat/hashstore
   ├── hashstore.yaml
   ├── objects
   |   ├── 4d
   |   │   └── 19
   |   │       └── 8171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c
   |   ├── 94
   |   │   └── f9
   |   │       └── b6c88f1f458e410c30c351c6384ea42ac1b5ee1f8430d3e365e43b78a38a
   |   └── 44
   |       └── 73
   |           └── 516a592209cbcd3a7ba4edeebbdb374ee8e4a49d19896fafb8f278dc25fa
   └── metadata
   |   ├── 0d
   |   │   └── 55
   |   │       └── 555ed77052d7e166017f779cbc193357c3a5006ee8b8457230bcf7abcef65e
   |   |           └── sha256(pid+formatId_sysmeta)
   |   |           └── sha256(pid+formatId_annotations)
   |   ├── a8
   |   │   └── 24
   |   │       └── 1925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf
   |   |           └── sha256(pid+formatId_sysmeta)
   |   |           └── sha256(pid+formatId_annotations)
   |   └── 7f
   |       └── 5c
   |           └── c18f0b04e812a3b4c8f686ce34e6fec558804bf61e54b176742a7f6368d6
   |               └── sha256(pid+formatId_sysmeta)
   |               └── sha256(pid+formatId_annotations)
   └── refs
       ├── cid
       |   └── 4d
       |   |   └── 19
       |   |       └── 8171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c
       |   ├── 94
       |   │   └── f9
       |   │       └── b6c88f1f458e410c30c351c6384ea42ac1b5ee1f8430d3e365e43b78a38a
       |   └── 44
       |       └── 73
       |           └── 516a592209cbcd3a7ba4edeebbdb374ee8e4a49d19896fafb8f278dc25fa
       └── pid
           └── 0d
           |   └── 55
           |       └── 555ed77052d7e166017f779cbc193357c3a5006ee8b8457230bcf7abcef65e
           ├── a8
           │   └── 24
           │       └── 1925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf
           └── 7f
               └── 5c
                   └── c18f0b04e812a3b4c8f686ce34e6fec558804bf61e54b176742a7f6368d6

**PID-based Access (Manually Find Objects)**:

Given a `PID` and a `formatId` (ex. "http://ns.dataone.org/service/types/v2.0"), it
is possible to access both the system metadata for an object and the bytes
of the object itself without any further store of information.

The procedure for this without using the HashStore Public API is as follows:

    1) Find the content identifier: Given the `PID`, calculate the SHA-256 hash,
    and base64-encode it to find the location of the pid refs file in 'refs/pid',
    which contains the content identifier associated with the `PID`.

    2) Find the metadata document: Use the SHA-256 hash of the `PID` to locate the
    corresponding folder in the `metadata` directory. The desired document's file
    name is formed by the hash of the `pid` + `formatId`. Open and read the metadata
    content from this file.

    3) With the content identifier of the given PID, open and read the data from the
    `objects` tree. With the hash of the `PID` + `formatId`, open and read data from
    the `metadata` tree.

    And as an example, the HashStore Public API equivalent commands would be as follows:

    1) Get the content identifier or check that an object exists:

       hashstore.find_object(pid)

    2) Get a stream to the system metadata to read from:

       hashstore.retrieve_metadata(pid, format_id)

    3) Get a stream to the object to read from:

       hashstore.retrieve_object(pid)

Public API
~~~~~~~~~~~~~~~~~~~

While Metacat will primarily handle read/write operations, other services like MetaDig and
DataONE MNs may interact with the hashstore directly. Below are the public methods implemented
in the Python and Java HashStore library. For detailed information on how each method should
work, please see the respective HashStore interface class in the projects below:

 (Python) https://github.com/DataONEorg/hashstore

 (Java) https://github.com/DataONEorg/hashstore-java

**The methods below will be included in the public API:**

+--------------------+------------------------------+----------------------------------+---------------------------------------------+
|     **Method**     |           **Args**           |         **Return Type**          |                  **Notes**                  |
+====================+==============================+==================================+=============================================+
| store_object       | pid, data, ...               | hash_address (object_cid, ...)   | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| tag_object         | pid, cid                     | boolean                          | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| verify_object      | object_info, checksum, ...   | void                             | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| find_object        | pid                          | string (cid)                     | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| store_metadata     | sysmeta, pid, format_id      | metadata_cid                     | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| retrieve_object    | pid                          | io.BufferedIOBase                | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| retrieve_metadata  | pid, format_id               | string (metadata)                | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| delete_object      | pid                          | boolean                          | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| delete_metadata    | pid, format_id               | boolean                          | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+
| get_hex_digest     | pid, algorithm               | string (hex_digest)              | Pending Review                              |
+--------------------+------------------------------+----------------------------------+---------------------------------------------+

 Below is a quick summary of how the Public API methods work:

 - **store_object**: Stores an object to HashStore with a given pid and data stream.

   - If a pid is available, it should not only tag the object with the pid, but verify
   that the reference files exist and correctly associated/tagged.
   - If a pid is unavailable, it should store the object. The calling app or method
   should then call 'tag_object' to create the necessary references, and then
   'verify_object' to confirm that everything exists where it should be.
 - **tag_object**: Tag an object by creating reference files
 - **verify_object**: Verifies that the values provided match
 - **find_object**: Check that an object exists, and if it does - return the content identifier
 - **store_metadata**: Store a metadata document to HashStore with a pid and format_id
 - **retrieve_object**: Returns a stream to the object if it exists
 - **retrieve_metadata**: Returns a stream to the metadata if it exists
 - **delete_object**: Deletes the pid reference file, removes the pid from the cid reference
   file and only deletes the object if the cid reference files does not have any references
 - **delete_metadata**: Deletes a metadata document
 - **get_hex_digest**: Returns the hex digest (hash value, checksum) of the algorithm desired
   of an object if it exists in HashStore.

.. figure:: images/hashstore_publicapi_mermaid_store_object_v2.png
   :figclass: top
   :align: center

   Figure 2. Mermaid diagram illustrating the initial store_object flow
   

Annotations
~~~~~~~~~~~~~~~~~~~
   
To support a paging API/query service to parse large data packages, we are proposing that
each member (subject) in a dataset package will have a RDF annotation file formatted in either
N-Triples or JSON-LD. Each annotation file can contain multiple triples that describe the
subject's relationships with its respective objects. These files will exist alongside 
sysmeta documents in HashStore, and can be stored, retrieved and deleted with the same public
API. In the diagram below, we illustrate this concept:

.. figure:: images/hashstore_annotation_initial_flow_chart.png
   :figclass: top
   :align: center

Notes:
   - Each annotation file (prefixed by ANNO-) contains triples about a subject, and the dotted line
     ending with an arrow represents where the annotation file would exist in HashStore
   - Ex. 'dou.mok.1' has 5 triples, describing what package it belongs to, that it's an object that can
     be found in HashStore, with a sysmeta document that can also be found in HashStore and a checksum
     and checksum algorithm.

To actually query the annotation graph, we will require an indexer that can not only index these
annotation files, but also be able to merge what's in additional ORE and EML documents to provide
completeness to the graph. This index serves as the basis for the query service that MetacatUI/front-end
would access. Users would not be accessing HashStore/Metacat directly, but this indexed version instead.


Virtual File Layout
~~~~~~~~~~~~~~~~~~~

In both of these cases, the main presentation of the directory layout would be
via a virtual layout that uses human-readable names and a directory structure
derived from the `prov:atLocation` metadata in our packages. Because a single 
file can be a member of multiple packages (both different versions of the same
package, and totally independent packages), there is a 1:many mapping between
files and packages. In addition, a file may be in different locations in these
various packages of which it is a part. So, our 'virtual' view will be derived 
from the metadata for a package, and will enable us to browse through the 
contents of the package independently of its physical layout.

Within the virtual package display, the main data directory will be reflected 
at the root of the tree, with a hidden `.metadata` directroy containing all of
the metadata files.

.. figure:: images/indexing-22.jpg
   :align: center
   
   Virtual file layout.

This layout will be familiar to researchers, but differs somewhat from the BagIt format
used for laying out data packages. In the BagIt approach, metadata files are stored
at the root of the folder structure, and files are held in a `data` subdirectory. Ideally, 
we could hide the BagIt metadata manifests in a hidden directory and keep the main
files at the root.

Filesystem Mounts
~~~~~~~~~~~~~~~~~

To support processing these files, we want to mount the data on various virtual 
machines and nodes in the Kubernetes cluster so that mutliple processors can 
seamlessly update and access the data. Thus our plan is to use a shared virtual 
filesystem. Reading from and writing to the shared virtual filesystem will result 
in reads and writes against the files in the physical layout using checksums.


Legacy MN Indexing
~~~~~~~~~~~~~~~~~~~

.. figure:: images/MN-indexing-hazelcast.png
   :align: center

   This sequence diagram shows the member node indexing message/data flow as it existed before the Metacat Storage/Indexing subsystem.
   The red boxes represent components that could be replaced by RabbitMQ messaging.

..
  @startuml images/MN-indexing-hazelcast.png
   title "Metacat indexing (MN w/Hazelcast)"
      participant Client
      participant "MNResourceHandler" <<Metacat>>
      participant "MNodeService" <<Metacat>>
      participant "D1NodeService" <<Metacat>>
      participant "MetacatHandler" <<Metacat>>
      participant "MetacatSolrIndex" <<Metacat>> #tomato
      queue       "hzIndexQueue" <<Hazelcast>> #tomato
      participant "SystemMetadataEventListener" <<Metacat-index>> #tomato
      participant "SolrIndex" <<Metacat-index>>
  
      Client -> MNResourceHandler : HTTP POST(sysmeta, pid, object) 
      
      activate MNResourceHandler 
      MNResourceHandler -> MNResourceHandler : handle(bytes)
      MNResourceHandler -> MNResourceHandler : putObject(pid, action)
      MNResourceHandler -> MNodeService : create(session, pid, object, sysmeta)
      deactivate MNResourceHandler
      
      activate MNodeService
      MNodeService -> D1NodeService : insertOrUpdateDocument(object, pid)
      deactivate MNodeService
  
      activate D1NodeService
      D1NodeService -> MetacatHandler : handleInsertOrUpdateAction()
      deactivate D1NodeService
      
      activate MetacatHandler
      MetacatHandler -> MetacatSolrIndex : submit(pid, sysmeta)
      deactivate MetacatHandler
      
      activate MetacatSolrIndex
      MetacatSolrIndex -> "hzIndexQueue" : put(pid, task)
      deactivate MetacatSolrIndex
      
      activate hzIndexQueue
      hzIndexQueue -> SystemMetadataEventListener : entryAdded(entryEvent)
      deactivate hzIndexQueue
      
      activate SystemMetadataEventListener
      SystemMetadataEventListener -> SolrIndex : update(pid, sysmeta);
      SystemMetadataEventListener -> SolrIndex : insertFields(pid, fields)
      deactivate SystemMetadataEventListener
      
  @enduml

Legacy CN Indexing 
~~~~~~~~~~~~~~~~~~~

.. figure:: images/CN-indexing-hazelcast.png

  This sequence diagram shows the coordinating legacy indexing message/data flow as implemented before the Metacat Storage/Indexing subsystem.
  The red boxes represent compoenents that could be replaced by RabbitMQ messaging.

..
  @startuml images/CN-indexing-hazelcast.png
   title "DataONE indexing (CN w/Hazelcast)"
      participant "task" <<d1_synchronization>>
      participant "CNCore" <<d1_synchronization>>
      participant "CNodeService" <<metacat>>
      participant "D1NodeService" <<metacat>>
      participant "MetacatHandler" <<metacat>>
      participant "systemMetadataMap" <<Hazelcast>> #tomato
      participant "IndexTaskGenerator" <<d1_cn_index_generator>> #tomato
      participant "IndexTaskRepository" <<d1_cn_index_common>> #tomato
      database    "index task queue" <<PostgreSQL>> #tomato
      participant "IndexTaskProcessor" <<d1_cn_index_processor>>
      participant "IndexTaskUpdateProcessor" <<d1_cn_index_processor>>
      
      activate task
      task -> CNCore : create(session, pid, object, sysmeta)
      deactivate task
      
      activate CNCore
      CNCore -> CNodeService : create (session, pid, object, sysmeta) 
      deactivate CNCore
      
      activate CNodeService 
      CNodeService -> D1NodeService : create (session, pid, object, sysmeta) 
      deactivate CNodeService
      
      activate D1NodeService
      D1NodeService -> D1NodeService : insertOrUpdate(session, pid, object, sysmeta) 
      D1NodeService -> MetacatHandler : handleInsertOrUpdate(ipAddress, ...)
      deactivate D1NodeService
      
      activate MetacatHandler
      MetacatHandler -> systemMetadataMap : put(pid, sysmeta)
      deactivate MetacatHandler
      
      activate systemMetadataMap
      systemMetadataMap -> IndexTaskGenerator : entryAdded(event)
      deactivate systemMetadataMap
      
      activate IndexTaskGenerator
      IndexTaskGenerator -> IndexTaskGenerator : processSystemMetaDataAdd(event, objectPath)
      IndexTaskGenerator -> IndexTaskRepository : repo.save(task(smd, String objectPath))
      deactivate IndexTaskGenerator
      
      activate IndexTaskRepository
      IndexTaskRepository -> "index task queue" : task(smd, String objectPath
      deactivate IndexTaskRepository
      
      activate "index task queue"
      "index task queue" -> IndexTaskProcessor : processIndexTaskQueue()
      deactivate "index task queue"
      
      activate IndexTaskProcessor
      IndexTaskProcessor -> IndexTaskProcessor : processTaskOnThread(task)
      IndexTaskProcessor -> IndexTaskProcessor : processTask(task)
      IndexTaskProcessor -> IndexTaskUpdateProcessor : process(task)
      deactivate IndexTaskProcessor
          
      activate IndexTaskUpdateProcessor
      IndexTaskUpdateProcessor -> SolrIndexService : insertIntoIndex(pid, sysmeta, objectPath)
      deactivate IndexTaskUpdateProcessor
      
  @enduml

Proposed Indexing with RabbitMQ
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. figure:: images/MN-indexing-rabbitmq.png
   :align: center

   This sequence diagram shows indexing message/data flow as it could be refactored to use RabbitMQ.
   The green boxes represent compenents that would replace legacy indexing components in order to implement RabbitMQ messaging.

..
  @startuml images/MN-indexing-rabbitmq.png
  title "Metacat indexing with RabbitMQ"
  participant Client
  participant "MNResourceHandler" <<Metacat>>
  participant "MNodeService" <<Metacat>>
  participant "D1NodeService" <<Metacat>>
  participant "MetacatHandler" <<Metacat>>
  participant "TaskGenerator" <<Metacat>> #lightgreen
  queue       "channel" <<RabbitMQ>> #lightgreen
  participant "IndexWorker" <<dataone-index>> #lightgreen
  participant "SolrIndex" <<dataone-index>>

  Client -> MNResourceHandler : HTTP POST(sysmeta, pid, object)

  activate MNResourceHandler
  MNResourceHandler -> MNResourceHandler : handle(bytes)
  MNResourceHandler -> MNResourceHandler : putObject(pid, action)
  MNResourceHandler -> MNodeService : create(session, pid, object, sysmeta)

  activate MNodeService
  MNodeService -> D1NodeService : insertOrUpdateDocument(object, pid)

  activate D1NodeService
  D1NodeService -> MetacatHandler : handleInsertOrUpdateAction()

  activate MetacatHandler
  MetacatHandler -> TaskGenerator : queueEntry(pid, sysmeta)

  activate TaskGenerator

  activate channel
  TaskGenerator -> channel : basicPublish(exchange, key, properties, message)
  TaskGenerator <- channel

  MetacatHandler <- TaskGenerator
  deactivate channel
  deactivate TaskGenerator

  D1NodeService <- MetacatHandler
  deactivate MetacatHandler

  MNodeService <- D1NodeService
  deactivate D1NodeService

  MNResourceHandler <- MNodeService
  deactivate MNodeService

  Client <- MNResourceHandler
  deactivate MNResourceHandler

  activate channel
  channel -> IndexWorker : handleDelivery(tag, envelope, properties, message)
  deactivate channel

  activate IndexWorker
  IndexWorker -> SolrIndex : update(pid, sysmeta, fields)
  IndexWorker <- SolrIndex
  IndexWorker -> SolrIndex : insertFields(pid, fields)
  IndexWorker <- SolrIndex
  IndexWorker -> channel : basicAck(envelope.getDeliveryTag(), false);
  deactivate IndexWorker
  @enduml

Proposed MN.create method
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. figure:: images/MN-create.png
   :align: center

   This sequence diagram shows data flow in the MN.create method.
   
..
   @startuml images/MN-create.png
   title "Sequence diagram for the MN.create method "
    participant Client
    participant "MNResourceHandler" <<Metacat>>
    participant "StreamMultipartHandler" <<Metacat>>
    participant "MetacatHandler" <<Metacat>>
    participant "XMLValidator" <<Metacat>>
    participant "StorageManager" <<Metacat Singleton>>
    /'participant "CephFileSystem" <<CEPH>>'/
    participant "MNodeService" <<Metacat>> 
    participant "D1AuthHelper" <<Metacat>>
    participant "ChecksumManager" <<Metacat Singleton>>
    participant "DBSystemMetadataManager" <<Metacat Singletone>>

  
   

   Client -> MNResourceHandler : HTTP POST(sysmeta, pid, object) 

   activate MNResourceHandler 
   MNResourceHandler -> StreamMultipartHandler : resoloveMultipart(HttpServeletRequest)
   deactivate MNResourceHandler

   activate StreamMultipartHandler
   StreamMultipartHandler -> StreamMultipartHandler : parseSysmeta
   StreamMultipartHandler -> StreamMultipartHandler : parsePid
   StreamMultipartHandler -> MetacatHandler : writeTemp(object, checksumAlg[])
   deactivate StreamMultipartHandler 

   activate MetacatHandler
   MetacatHandler -> MetacatHandler : createDigestInputStream(object)
   MetacatHandler -> StorageManager : writeTemp(digestObjectInputStream)
   deactivate MetacatHandler

   activate StorageManager
   StorageManager -> MetacatHandler : objectFile
   deactivate StorageManager

   activate MetacatHandler
   MetacatHandler -> MetacatHandler : createCheckedFile(objectFile, checksums[]) 
   MetacatHandler -> StreamMultipartHandler : checkedFile
   deactivate MetacatHandler

   activate StreamMultipartHandler
   StreamMultipartHandler -> MNResourceHandler : MultipartRequest (sysmeta, pid, checkedFile)
   deactivate StreamMultipartHandler 

   activate MNResourceHandler 
   MNResourceHandler -> MNodeService: create(session, pid, checkedFileInputStream{with the field of checkedFile}, sysmeta)
   deactivate MNResourceHandler

   activate MNodeService
   MNodeService -> D1AuthHelper : doIsAuthorized(session)
   deactivate MNodeService

   activate D1AuthHelper
   D1AuthHelper -> MNodeService : true
   deactivate D1AuthHelper

   activate MNodeService
   MNodeService -> MetacatHandler : write(pid, checkedFileInputStream, sysmeta)
   deactivate MNodeService 

   alt "object is scientific metadata"
      activate MetacatHandler 
      MetacatHandler -> XMLValidator : validate(checkedFile)
      deactivate MetacatHandler
  
      activate XMLValidator
      XMLValidator -> MetacatHandler : true
      deactivate XMLValidator
      activate MetacatHandler
   end


   MetacatHandler -> StorageManager : mkDirs(pid-hash-string)
   deactivate MetacatHandler

   alt
      activate StorageManager 
      StorageManager -> MetacatHandler : directoryPath
      deactivate StorageManager

      activate MetacatHandler
      MetacatHandler -> StorageManager : move(checkedFile, diretoryPath)
      deactivate MetacatHandler

      activate StorageManager 
      StorageManager -> MetacatHandler : success
      deactivate StorageManager

      activate MetacatHandler 
      MetacatHandler -> StorageManager : write(sysmetaInputStream, directoryPath)
      deactivate MetacatHandler

      activate StorageManager 
      StorageManager -> MetacatHandler : success
      deactivate StorageManager

      activate MetacatHandler
      MetacatHandler -> ChecksumManager : map(pid, checksums[])
      deactivate MetacatHandler

      activate ChecksumManager
      ChecksumManager -> MetacatHandler : success
      deactivate ChecksumManager


   else directoryPath already exists

   /'MetacatHandler -> StorageManager : write(sysmetaInputStream)'/

   /'activate StorageManager '/
   /'StorageManager -> MetacatHandler : success'/
   /'deactivate StorageManager'/
   /'activate MetacatHandler'/
   /'MetacatHandler -> ChecksumManager : map(pid, checksum[])'/
   /'deactivate MetacatHandler./
   /'activate ChecksumManager./
   /'ChecksumManager -> MetacatHandler : success'/
      MetacatHandler -> Client : InvalidRequest(pid already used)
      deactivate ChecksumManager

   end

   activate MetacatHandler
   MetacatHandler -> DBSystemMetadataManager : save(sysmeta)
   deactivate MetacatHandler

   activate DBSystemMetadataManager
   DBSystemMetadataManager -> MetacatHandler : success
   deactivate DBSystemMetadataManager

   activate MetacatHandler
   MetacatHandler -> MNodeService : pid
   deactivate MetacatHandler

   activate MNodeService
   MNodeService -> MNResourceHandler : pid
   deactivate MNodeService

   activate MNResourceHandler
   MNResourceHandler -> Client : pid
   deactivate MNResourceHandler

   @enduml

CheckedFile and CheckedFileInputStream Class Diagram
~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~

.. figure:: images/checkedFile-class.png
   :align: center  
..
   @startuml images/checkedFile-class.png

   File <|-- CheckedFile
   CheckedFile : Checksum[]: checksums
   CheckedFile : getChecksums()

   FileInputStream <|-- CheckedFileInputStream
   CheckedFileInputStream : CheckedFile: checkedFile
   CheckedFileInputStream : getFile()

   @enduml
