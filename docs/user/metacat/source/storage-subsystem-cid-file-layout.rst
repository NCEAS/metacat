Physical File Layout, Hash-tree based
=====================================
   
For physical file storage and layout, our goal is to provide a consistent directory
structure that enables us to store each file once and only once, that will be
robust against naming issues such as illegal characters, and that allows us to
access both system metadata and the file contents knowing only the PID for an
object. This approach focuses on using a hash identifier for naming objects,
rather than an authority-based identifier such as a PID or SID.

**Raw File Storage**: The raw bytes of each object (data, metadata, or resource
map) are saved in a file that is named using a content identifier (CID) for that
set of bytes. This content identifier is created using a hashing algorithm such
that each unique set of bytes produces a unique checksum value. That checksum
value is then used to name the file. In this way, even when the same file is
uploaded multiple times, it will only be stored once in the filesystem.

**Checksum algorithm and encoding**

We have multiple hash algorithms to choose from, and each has multiple ways of
encoding the binary hash into a string representation. We will choose the
simplest, most common configuration which is to use a `SHA-256` hash
algorithm, with the binary digest converted to a string value using `base64`
encoding. That makes each hash value 64 characters long (representing
the 256 bit binary value). For example, here is a base16 hex-encoded SHA-256 value:

   4d198171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c

While we chose this common combination, we could also have chosen other hash
algorithms (e.g., SHA-1, SHA3-256, blake2b-512) and alternate string encodings
(e.g., base58, Multihash (https://multiformats.io/multihash/)). Multihash may be
a valuable approach to future-proof the storage system, because it enables use
of multiple checksum algorithms.

**Folder layout**

To reduce the number of files in a given directory, we use the first several
characters of the hash to create a directory hierarchy and divide the files up to
make the tree simpler to explore and less likely to exceed operating system
limits on files. We store all objects in an `objects` directory, with two
levels of depth and a 'width' of 2 digits. Because each digit in the hash can
contain 16 values, the directory structure can contain 65,536 subdirectories
(256^2).  To accommodate a larger number of directories, we could add another level or
two of depth to the hierarchy.  An example file layout for three objects would be::

   /var/metacat/objects
   ├── 15
   │   └── 52
   │       └── 5dda7121013bc3eba2e2d237a5ae70b291a461ca539053de75f33c9ac44c
   ├── 4d
   │   └── 19
   │       └── 8171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c
   └── 94
      └── f9
         └── b6c88f1f458e410c30c351c6384ea42ac1b5ee1f8430d3e365e43b78a38a

Note how the full hash value is obtained by appending the directory names with
the file name (e.g.,
`15525dda7121013bc3eba2e2d237a5ae70b291a461ca539053de75f33c9ac44c` for the first
object).

**Storing metadata** With this layout, knowing the hash value for a file allows
us to retrieve it. But it does not provide a mechanism to store metadata about
the object, including it's persistent identifier (PID), other system metadata
for the object, or extended metadata that we might want to include. So, in
addition to data objects, the system supports storage for metadata documents
that are associated with particular data objects. These metadata files are
stored as delimited files with a header and body section. The header contains
the 64 character hash of the data file described by this metadata, followed by a
space, then the `formatId` of the metadata format for the metadata in the file,
and then a NULL (`\x00`). This header is then followed by the content of the
metadata document in UTF-8 encoding (see example below). **This metadata file is 
named using the SHA-256 hash of the persistent identifier (PID) of the object that 
it describes, and stored in a `sysmeta` directory parallel to the one described 
above, and structured analogously.**

For example, given the PID `jtao.1700.1`, one can calculate the SHA-256 of that PID using::

   $ echo -n "jtao.1700.1" | shasum -a 256
   a8241925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf

So, the system metadata file would be stored at
`sysmeta/a8/24/1925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf` using the
file format described above. Extending our diagram from above, we now see the three 
hashes that represent data files, along with three that represent system metadata files 
named with the hash of the PID they describe::

   /var/metacat
   ├── objects
   │   ├── 15
   │   │   └── 52
   │   │       └── 5dda7121013bc3eba2e2d237a5ae70b291a461ca539053de75f33c9ac44c
   │   ├── 4d
   │   │   └── 19
   │   │       └── 8171eef969d553d4c9537b1811a7b078f9a3804fc978a761bc014c05972c
   │   └── 94
   │       └── f9
   │           └── b6c88f1f458e410c30c351c6384ea42ac1b5ee1f8430d3e365e43b78a38a
   └── sysmeta
      ├── 7f
      │   └── 5c
      │       └── c18f0b04e812a3b4c8f686ce34e6fec558804bf61e54b176742a7f6368d6
      ├── a8
      │   └── 24
      │       └── 1925740d5dcd719596639e780e0a090c9d55a5d0372b0eaf55ed711d4edf
      └── f6
         └── fa
               └── c7b713ca66b61ff1c3c8259a8b98f6ceab30b906e42a24fa447db66fa8ba

**PID-based access**:  Given a PID, we can discover and access both the system
metadata for an object and the bytes of the object itself without any further
store of information. The procedure for this is as follows:

1) given the PID, calculate the SHA-256 hash, and base64-encode it to find the `metadata hash`
2) Use the `metadata hash` to locate and read the metadata file from the `sysmeta` tree
    - parse the header to extract the content identifier (`cid`) and the `formatId`
    - read the remaining body of the document to obtain the `sysmeta`, which includes format information about the data object
3) with the `cid`, open and read the data from the `objects` tree

The structure of the system metadata document along with it's header is shown in hex
format below for reference purposes. Note the presence of the NULL `\x00` character in 
the first line following the format identifier. Parsers can use this first `\x00` NULL 
to delimit the header from the body of the metadata document. This particular metadata
document is for the PID `doi:10.18739_A2901ZH2M`, which is stored in the file 
`sysmeta/f6/fa/c7b713ca66b61ff1c3c8259a8b98f6ceab30b906e42a24fa447db66fa8ba` 
based on the SAH-256 of the PID of the data object::

   ┌────────┬─────────────────────────┬─────────────────────────┬────────┬────────┐
   │00000000│ 34 64 31 39 38 31 37 31 ┊ 65 65 66 39 36 39 64 35 │4d198171┊eef969d5│
   │00000010│ 35 33 64 34 63 39 35 33 ┊ 37 62 31 38 31 31 61 37 │53d4c953┊7b1811a7│
   │00000020│ 62 30 37 38 66 39 61 33 ┊ 38 30 34 66 63 39 37 38 │b078f9a3┊804fc978│
   │00000030│ 61 37 36 31 62 63 30 31 ┊ 34 63 30 35 39 37 32 63 │a761bc01┊4c05972c│
   │00000040│ 20 68 74 74 70 3a 2f 2f ┊ 6e 73 2e 64 61 74 61 6f │ http://┊ns.datao│
   │00000050│ 6e 65 2e 6f 72 67 2f 73 ┊ 65 72 76 69 63 65 2f 74 │ne.org/s┊ervice/t│
   │00000060│ 79 70 65 73 2f 76 32 2e ┊ 30 00 3c 3f 78 6d 6c 20 │ypes/v2.┊00<?xml │
   │00000070│ 76 65 72 73 69 6f 6e 3d ┊ 22 31 2e 30 22 20 65 6e │version=┊"1.0" en│
   │00000080│ 63 6f 64 69 6e 67 3d 22 ┊ 55 54 46 2d 38 22 20 73 │coding="┊UTF-8" s│
   │00000090│ 74 61 6e 64 61 6c 6f 6e ┊ 65 3d 22 79 65 73 22 3f │tandalon┊e="yes"?│
   │000000a0│ 3e 0a 3c 6e 73 33 3a 73 ┊ 79 73 74 65 6d 4d 65 74 │>_<ns3:s┊ystemMet│
   │000000b0│ 61 64 61 74 61 20 78 6d ┊ 6c 6e 73 3a 6e 73 32 3d │adata xm┊lns:ns2=│
   │000000c0│ 22 68 74 74 70 3a 2f 2f ┊ 6e 73 2e 64 61 74 61 6f │"http://┊ns.datao│
   │000000d0│ 6e 65 2e 6f 72 67 2f 73 ┊ 65 72 76 69 63 65 2f 74 │ne.org/s┊ervice/t│
   │000000e0│ 79 70 65 73 2f 76 31 22 ┊ 20 78 6d 6c 6e 73 3a 6e │ypes/v1"┊ xmlns:n│
   │000000f0│ 73 33 3d 22 68 74 74 70 ┊ 3a 2f 2f 6e 73 2e 64 61 │s3="http┊://ns.da│
   │00000100│ 74 61 6f 6e 65 2e 6f 72 ┊ 67 2f 73 65 72 76 69 63 │taone.or┊g/servic│
   │00000110│ 65 2f 74 79 70 65 73 2f ┊ 76 32 2e 30 22 3e 0a 20 │e/types/┊v2.0">_ │
   │00000120│ 20 20 20 3c 73 65 72 69 ┊ 61 6c 56 65 72 73 69 6f │   <seri┊alVersio│
   │00000130│ 6e 3e 30 3c 2f 73 65 72 ┊ 69 61 6c 56 65 72 73 69 │n>0</ser┊ialVersi│
   │00000140│ 6f 6e 3e 0a 20 20 20 20 ┊ 3c 69 64 65 6e 74 69 66 │on>_    ┊<identif│
   │00000150│ 69 65 72 3e 64 6f 69 3a ┊ 31 30 2e 31 38 37 33 39 │ier>doi:┊10.18739│
   │00000160│ 2f 41 32 39 30 31 5a 48 ┊ 32 4d 3c 2f 69 64 65 6e │/A2901ZH┊2M</iden│
   │00000170│ 74 69 66 69 65 72 3e 0a ┊ 20 20 20 20 3c 66 6f 72 │tifier>_┊    <for│
   │00000180│ 6d 61 74 49 64 3e 68 74 ┊ 74 70 3a 2f 2f 77 77 77 │matId>ht┊tp://www│
   │00000190│ 2e 69 73 6f 74 63 32 31 ┊ 31 2e 6f 72 67 2f 32 30 │.isotc21┊1.org/20│
   │000001a0│ 30 35 2f 67 6d 64 3c 2f ┊ 66 6f 72 6d 61 74 49 64 │05/gmd</┊formatId│
   │000001b0│ 3e 0a 20 20 20 20 3c 73 ┊ 69 7a 65 3e 33 39 39 39 │>_    <s┊ize>3999│
   │000001c0│ 33 3c 2f 73 69 7a 65 3e ┊ 0a 20 20 20 20 3c 63 68 │3</size>┊_    <ch│
   │000001d0│ 65 63 6b 73 75 6d 20 61 ┊ 6c 67 6f 72 69 74 68 6d │ecksum a┊lgorithm│
   │000001e0│ 3d 22 53 48 41 2d 32 35 ┊ 36 22 3e 34 64 31 39 38 │="SHA-25┊6">4d198│
   │000001f0│ 31 37 31 65 65 66 39 36 ┊ 39 64 35 35 33 64 34 63 │171eef96┊9d553d4c│
   │00000200│ 39 35 33 37 62 31 38 31 ┊ 31 61 37 62 30 37 38 66 │9537b181┊1a7b078f│
   │00000210│ 39 61 33 38 30 34 66 63 ┊ 39 37 38 61 37 36 31 62 │9a3804fc┊978a761b│
   │00000220│ 63 30 31 34 63 30 35 39 ┊ 37 32 63 3c 2f 63 68 65 │c014c059┊72c</che│
   │00000230│ 63 6b 73 75 6d 3e 0a 20 ┊ 20 20 20 3c 73 75 62 6d │cksum>_ ┊   <subm│
   │00000240│ 69 74 74 65 72 3e 68 74 ┊ 74 70 3a 2f 2f 6f 72 63 │itter>ht┊tp://orc│
   │00000250│ 69 64 2e 6f 72 67 2f 30 ┊ 30 30 30 2d 30 30 30 33 │id.org/0┊000-0003│
   │00000260│ 2d 34 37 30 33 2d 31 39 ┊ 37 34 3c 2f 73 75 62 6d │-4703-19┊74</subm│
   │00000270│ 69 74 74 65 72 3e 0a 20 ┊ 20 20 20 3c 72 69 67 68 │itter>_ ┊   <righ│
   │00000280│ 74 73 48 6f 6c 64 65 72 ┊ 3e 68 74 74 70 3a 2f 2f │tsHolder┊>http://│
   │00000290│ 6f 72 63 69 64 2e 6f 72 ┊ 67 2f 30 30 30 30 2d 30 │orcid.or┊g/0000-0│
   │000002a0│ 30 30 33 2d 34 37 30 33 ┊ 2d 31 39 37 34 3c 2f 72 │003-4703┊-1974</r│
   │000002b0│ 69 67 68 74 73 48 6f 6c ┊ 64 65 72 3e 0a 20 20 20 │ightsHol┊der>_   │
   │000002c0│ 20 3c 61 63 63 65 73 73 ┊ 50 6f 6c 69 63 79 3e 0a │ <access┊Policy>_│
   │000002d0│ 20 20 20 20 20 20 20 20 ┊ 3c 61 6c 6c 6f 77 3e 0a │        ┊<allow>_│
   │000002e0│ 20 20 20 20 20 20 20 20 ┊ 20 20 20 20 3c 73 75 62 │        ┊    <sub│
   │000002f0│ 6a 65 63 74 3e 70 75 62 ┊ 6c 69 63 3c 2f 73 75 62 │ject>pub┊lic</sub│
   │00000300│ 6a 65 63 74 3e 0a 20 20 ┊ 20 20 20 20 20 20 20 20 │ject>_  ┊        │
   │00000310│ 20 20 3c 70 65 72 6d 69 ┊ 73 73 69 6f 6e 3e 72 65 │  <permi┊ssion>re│
   │00000320│ 61 64 3c 2f 70 65 72 6d ┊ 69 73 73 69 6f 6e 3e 0a │ad</perm┊ission>_│
   │00000330│ 20 20 20 20 20 20 20 20 ┊ 3c 2f 61 6c 6c 6f 77 3e │        ┊</allow>│
   │00000340│ 0a 20 20 20 20 20 20 20 ┊ 20 3c 61 6c 6c 6f 77 3e │_       ┊ <allow>│
   │00000350│ 0a 20 20 20 20 20 20 20 ┊ 20 20 20 20 20 3c 73 75 │_       ┊     <su│
   │00000360│ 62 6a 65 63 74 3e 43 4e ┊ 3d 61 72 63 74 69 63 2d │bject>CN┊=arctic-│
   │00000370│ 64 61 74 61 2d 61 64 6d ┊ 69 6e 73 2c 44 43 3d 64 │data-adm┊ins,DC=d│
   │00000380│ 61 74 61 6f 6e 65 2c 44 ┊ 43 3d 6f 72 67 3c 2f 73 │ataone,D┊C=org</s│
   │00000390│ 75 62 6a 65 63 74 3e 0a ┊ 20 20 20 20 20 20 20 20 │ubject>_┊        │
   │000003a0│ 20 20 20 20 3c 70 65 72 ┊ 6d 69 73 73 69 6f 6e 3e │    <per┊mission>│
   │000003b0│ 72 65 61 64 3c 2f 70 65 ┊ 72 6d 69 73 73 69 6f 6e │read</pe┊rmission│
   │000003c0│ 3e 0a 20 20 20 20 20 20 ┊ 20 20 20 20 20 20 3c 70 │>_      ┊      <p│
   │000003d0│ 65 72 6d 69 73 73 69 6f ┊ 6e 3e 77 72 69 74 65 3c │ermissio┊n>write<│
   │000003e0│ 2f 70 65 72 6d 69 73 73 ┊ 69 6f 6e 3e 0a 20 20 20 │/permiss┊ion>_   │
   │000003f0│ 20 20 20 20 20 20 20 20 ┊ 20 3c 70 65 72 6d 69 73 │        ┊ <permis│
   │00000400│ 73 69 6f 6e 3e 63 68 61 ┊ 6e 67 65 50 65 72 6d 69 │sion>cha┊ngePermi│
   │00000410│ 73 73 69 6f 6e 3c 2f 70 ┊ 65 72 6d 69 73 73 69 6f │ssion</p┊ermissio│
   │00000420│ 6e 3e 0a 20 20 20 20 20 ┊ 20 20 20 3c 2f 61 6c 6c │n>_     ┊   </all│
   │00000430│ 6f 77 3e 0a 20 20 20 20 ┊ 3c 2f 61 63 63 65 73 73 │ow>_    ┊</access│
   │00000440│ 50 6f 6c 69 63 79 3e 0a ┊ 20 20 20 20 3c 72 65 70 │Policy>_┊    <rep│
   │00000450│ 6c 69 63 61 74 69 6f 6e ┊ 50 6f 6c 69 63 79 20 72 │lication┊Policy r│
   │00000460│ 65 70 6c 69 63 61 74 69 ┊ 6f 6e 41 6c 6c 6f 77 65 │eplicati┊onAllowe│
   │00000470│ 64 3d 22 66 61 6c 73 65 ┊ 22 20 6e 75 6d 62 65 72 │d="false┊" number│
   │00000480│ 52 65 70 6c 69 63 61 73 ┊ 3d 22 30 22 3e 0a 20 20 │Replicas┊="0">_  │
   │00000490│ 20 20 20 20 20 20 3c 62 ┊ 6c 6f 63 6b 65 64 4d 65 │      <b┊lockedMe│
   │000004a0│ 6d 62 65 72 4e 6f 64 65 ┊ 3e 75 72 6e 3a 6e 6f 64 │mberNode┊>urn:nod│
   │000004b0│ 65 3a 4b 4e 42 3c 2f 62 ┊ 6c 6f 63 6b 65 64 4d 65 │e:KNB</b┊lockedMe│
   │000004c0│ 6d 62 65 72 4e 6f 64 65 ┊ 3e 0a 20 20 20 20 20 20 │mberNode┊>_      │
   │000004d0│ 20 20 3c 62 6c 6f 63 6b ┊ 65 64 4d 65 6d 62 65 72 │  <block┊edMember│
   │000004e0│ 4e 6f 64 65 3e 75 72 6e ┊ 3a 6e 6f 64 65 3a 6d 6e │Node>urn┊:node:mn│
   │000004f0│ 55 43 53 42 31 3c 2f 62 ┊ 6c 6f 63 6b 65 64 4d 65 │UCSB1</b┊lockedMe│
   │00000500│ 6d 62 65 72 4e 6f 64 65 ┊ 3e 0a 20 20 20 20 3c 2f │mberNode┊>_    </│
   │00000510│ 72 65 70 6c 69 63 61 74 ┊ 69 6f 6e 50 6f 6c 69 63 │replicat┊ionPolic│
   │00000520│ 79 3e 0a 20 20 20 20 3c ┊ 61 72 63 68 69 76 65 64 │y>_    <┊archived│
   │00000530│ 3e 66 61 6c 73 65 3c 2f ┊ 61 72 63 68 69 76 65 64 │>false</┊archived│
   │00000540│ 3e 0a 20 20 20 20 3c 64 ┊ 61 74 65 55 70 6c 6f 61 │>_    <d┊ateUploa│
   │00000550│ 64 65 64 3e 32 30 32 31 ┊ 2d 31 31 2d 30 32 54 32 │ded>2021┊-11-02T2│
   │00000560│ 33 3a 30 38 3a 32 30 2e ┊ 37 37 30 2b 30 30 3a 30 │3:08:20.┊770+00:0│
   │00000570│ 30 3c 2f 64 61 74 65 55 ┊ 70 6c 6f 61 64 65 64 3e │0</dateU┊ploaded>│
   │00000580│ 0a 20 20 20 20 3c 64 61 ┊ 74 65 53 79 73 4d 65 74 │_    <da┊teSysMet│
   │00000590│ 61 64 61 74 61 4d 6f 64 ┊ 69 66 69 65 64 3e 32 30 │adataMod┊ified>20│
   │000005a0│ 32 31 2d 31 31 2d 30 32 ┊ 54 32 33 3a 30 38 3a 32 │21-11-02┊T23:08:2│
   │000005b0│ 30 2e 37 37 30 2b 30 30 ┊ 3a 30 30 3c 2f 64 61 74 │0.770+00┊:00</dat│
   │000005c0│ 65 53 79 73 4d 65 74 61 ┊ 64 61 74 61 4d 6f 64 69 │eSysMeta┊dataModi│
   │000005d0│ 66 69 65 64 3e 0a 20 20 ┊ 20 20 3c 6f 72 69 67 69 │fied>_  ┊  <origi│
   │000005e0│ 6e 4d 65 6d 62 65 72 4e ┊ 6f 64 65 3e 75 72 6e 3a │nMemberN┊ode>urn:│
   │000005f0│ 6e 6f 64 65 3a 6d 6e 54 ┊ 65 73 74 41 52 43 54 49 │node:mnT┊estARCTI│
   │00000600│ 43 3c 2f 6f 72 69 67 69 ┊ 6e 4d 65 6d 62 65 72 4e │C</origi┊nMemberN│
   │00000610│ 6f 64 65 3e 0a 20 20 20 ┊ 20 3c 61 75 74 68 6f 72 │ode>_   ┊ <author│
   │00000620│ 69 74 61 74 69 76 65 4d ┊ 65 6d 62 65 72 4e 6f 64 │itativeM┊emberNod│
   │00000630│ 65 3e 75 72 6e 3a 6e 6f ┊ 64 65 3a 6d 6e 54 65 73 │e>urn:no┊de:mnTes│
   │00000640│ 74 41 52 43 54 49 43 3c ┊ 2f 61 75 74 68 6f 72 69 │tARCTIC<┊/authori│
   │00000650│ 74 61 74 69 76 65 4d 65 ┊ 6d 62 65 72 4e 6f 64 65 │tativeMe┊mberNode│
   │00000660│ 3e 0a 20 20 20 20 3c 66 ┊ 69 6c 65 4e 61 6d 65 3e │>_    <f┊ileName>│
   │00000670│ 6d 65 74 61 64 61 74 61 ┊ 2e 78 6d 6c 3c 2f 66 69 │metadata┊.xml</fi│
   │00000680│ 6c 65 4e 61 6d 65 3e 0a ┊ 3c 2f 6e 73 33 3a 73 79 │leName>_┊</ns3:sy│
   │00000690│ 73 74 65 6d 4d 65 74 61 ┊ 64 61 74 61 3e 0a       │stemMeta┊data>_  │
   └────────┴─────────────────────────┴─────────────────────────┴────────┴────────┘

**Other metadata types**: While we currently only have a need to access system
metadata for each object, in the future we envision potentially including other
metadata files that can be used for describing individual data objects. This
might include package relationships and other annotations that we wish to
include for each data file. To accommodate this, we could add another metadata
directory (e.g., `annotations`) as a sibling to the `objects` directory, and include
an additional metadata file using the same PID-based annotation approach described
above for system metadata. This enables the storage system to be used to store
arbitrary additional metadata in a structured and predictable way but that does not
require external database access to predict its location and type. Alternatively, we
could use mime-multipart or a similar multipart file encoding to include multiple
metadata files in the PID-encoded metadata file.

Hash Trees (aka Merkle trees)
-----------------------------

While we plan to hash whole objects as described above,
there also can be benefits of chunking data into smaller blocks and arranging
them as a Merkle tree for storage. See https://en.wikipedia.org/wiki/Merkle_tree
for an overview. Some of the features that might be useful for us:

- Blocks of files that are closely related (e.g,, from append-only versioned files) would share the same hash, and therefore require less storage
- Downloads can be fully parallelized across multiple interfaces/hosts for blocks
- Given the root hash of a merkle tree, one can download the children blocks from any source (distributed, untrusted)
- Given a complex set of objects, a single hash comparison of the root hash can quickly deduce whether two hash collections differ 
    - Proceeding down the tree and comparing sub-tree hashes can pinpoint where the trees differ
- In addition to representing a single "object" as a tree, we can also create other composite trees that represent multi-object collections, such as data packages
    - All of the benefits at the file level would also apply at the collection level

These features are used within existing systems like Git and IPFS to build fully
decentralized graphs of versioned content. While generating the CID for a leaf
node object is straightforward, these systems also provide mechanisms for graph
nodes to represent directory-level information, which itself is hashed and
becomes part of the graph. For example, in Git, each object is of type `blob`,
`tree`, `commit`, and `tag` (see
https://towardsdatascience.com/understanding-the-fundamentals-of-git-25b5b7ded3c4).
A `blob` represents the content the content of a file, and is named based on the
SHA-1 hash of its contents.  The actual content of a blob object is the string
`blob` followed by a space, the size of the file in bytes, a null `\0`
character, and then the zlib-compressed content of the original file.  In
contrast, a `tree` object represents metadata about a directory, and contains a
listing of all of the blobs and other tree objects in that directory, along with
their CIDs. That file itself is hashed and added to the object store, and
so incorporates by reference the CIDs of the files and directories it contains.
Finally, a `commit` object contains a pointer to the root tree object for the
directory and metadata about the commit itself, including its parent commit,
author, date, and message. These commit files are also hashed and included in
the object store. This simple structure of a graph of hash-derived content
identifiers allows a sophisticated and reliable version control system.

Finally, these blocks can be used within a Distributed Hash Table with hashes as
keys and data blocks as values (see
https://en.wikipedia.org/wiki/Distributed_hash_table#Structure) to build an
efficient search and discovery system for the nodes based on the key values.
This approach is the core for distributed systems like BitTorrent and IPFS.

Data packages as hash trees
---------------------------

First a review of Git...

.. figure:: images/hash-trees/hash-trees-35.jpg
   :align: center

   Git object storage as a hash-tree.


Design a Data Package layout with similar properties.

Start with making a hash tree from our BagIt file layout:

.. figure:: images/hash-trees/hash-trees-36.jpg
   :align: center

   BagIt-based hash tree.

Next move sysmeta, and then eliminate folder tree objects...

.. figure:: images/hash-trees/hash-trees-37.jpg
   :align: center

   Remove folder tree objects.

Add annotation files as siblings... and then replace trees with annotation files...

.. figure:: images/hash-trees/hash-trees-38.jpg
   :align: center

   Add annotation files.

Remove sysmeta from the hash tree so it is no longer versioned. Then try switching to PID-based identifiers.

.. figure:: images/hash-trees/hash-trees-39.jpg
   :align: center

   Switch to PID-hash file naming.

Switch back again to hash identifiers, and add our folder structure back using annotation objects as tree objects...

.. figure:: images/hash-trees/hash-trees-40.jpg
   :align: center

.. figure:: images/hash-trees/hash-trees-40-mermaid.png
   :align: center

.. mermaid:::
   flowchart TD
      H13["H13
         H13 type PACKAGE
         H13 contains H11
         H13 contains H12
         H13 contains H10"]
      subgraph ORE
         H12["H12
               H12 type ANNO"]
         H1["H1: ORE"]
         P1["P1: Sysmeta"]
         P1 --> H1
         H12 --> H1
      end
      subgraph EML
         H11["H11
               H11 type ANNO"]
         H2["H2: EML"]
         P2["P2: Sysmeta"]
         H11 --> H2
         P2 --> H2
      end
      H10["H10
         H10 type FOLDER
         H10 contains H6
         H10 contains H9"]
      H13 --> H12
      H13 --> H11
      H13 --> H10
      subgraph Blob1
         H6["H6
               H6 type ANNO
               H6 contains H3"]
         H3["H3: Data"]
         P3["P3: Sysmeta"]
         H6 --> H3
         P3 --> H3
      end    
      H10 --> H6
      H9["H9
         H9 type FOLDER
         H9 contains H8
         H9 contains H8"]
      H10 --> H9
      subgraph Blob2
         H7["H7
               H7 type ANNO
               H7 contains H4
               H4 type BLOB"]
         H4["H4: Data"]
         P4["P4: Sysmeta"]
         H7 --> H4
         P4 --> H4
      end
      H9 --> H7
      subgraph Blob3
         H8["H8
               H8 type ANNO
               H8 contains H5
               H5 type BLOB"]
         H5["H5: Data"]
         P5["P5: Sysmeta"]
         H8 --> H5
         P5 --> H5
      end
      H9 --> H8 
      classDef cyan fill:#7ff;
      class H13,H12,H11,H10,H6,H9,H7,H8 cyan
      classDef mage fill:#ff7ffe;
      class P1,P2,P3,P4,P5 mage
      classDef lime fill:#dfffda;
      class H1,H2 lime


Back to CIDs, with folders as annotation objects.

Revisit the directory layout for a Git-like structure that lets us store multiple data package versions in a single folder hierarchy...

.. figure:: images/hash-trees/hash-trees-41.jpg
   :align: center

   Git object storage as a hash-tree.

Data Information Package (DIP) prototype
----------------------------------------

See the [Data Info Package](https://github.com/mbjones/dip-noodling) repo for a quick proof-of-concept on some of these ideas.
This includes shell script functions for basic operations, such as storing a blob as a new leaf node, tagging items
by their CID, and listing and viewing various items in a package.


Delta changes
--------------

- [markdown_doc](https://hackmd.io/@nenuji/B1mE5FG-6)
