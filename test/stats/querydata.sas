OPTIONS LS=78 NOCENTER;
LIBNAME data '.';
FILENAME qdata './querydata.csv';

DATA ONE;
  INFILE qdata DLM=',' TRUNCOVER;
  INPUT nodes depth index nested hits searched;

DATA two;
  LENGTH method $ 8;
  SET one;
  rename index=qtime;
  method='index';
DATA three;
  LENGTH method $ 8;
  SET one;
  rename nested=qtime;
  method='nested';

DATA four;
  SET two three;
  DROP index nested;

PROC PRINT;

PROC SORT;
  BY method nodes depth hits searched;

PROC GLM;
  CLASSES method;
  MODEL qtime = method nodes depth searched;
QUIT;

PROC PLOT;
  PLOT qtime*depth;
PROC PLOT;
  PLOT qtime*nodes;
PROC PLOT;
  PLOT qtime*searched;
 
RUN;
