--1a
INSERT INTO USER_SDO_GEOM_METADATA 
VALUES(
    'FIGURY',
    'KSZTALT',
    MDSYS.SDO_DIM_ARRAY(
        MDSYS.SDO_DIM_ELEMENT('X', 0, 100, 0.01),
        MDSYS.SDO_DIM_ELEMENT('Y', 0, 100, 0.01)),
    NULL
);

--1b
SELECT SDO_TUNE.ESTIMATE_RTREE_INDEX_SIZE(3000000, 8192, 10, 2, 0)
FROM FIGURY
WHERE ROWNUM <= 1;

--1c
CREATE INDEX FIGURY_INDEX
ON FIGURY(KSZTALT)
INDEXTYPE IS MDSYS.SPATIAL_INDEX_V2;

--1d
SELECT ID
FROM FIGURY
WHERE SDO_FILTER(KSZTALT, SDO_GEOMETRY(2001, NULL, SDO_POINT_TYPE(3,3, NULL), NULL, NULL)) = 'TRUE';
-- wynik nie odpowiada rzeczywisto?ci -> operator SDO_FILTER wykorzystuje jedynie pierwsza faz? zapytania, czyli daje tylko POTENCJALNYCH kandydatów

--1e
SELECT ID
FROM FIGURY
WHERE SDO_RELATE(KSZTALT, SDO_GEOMETRY(2001, NULL, SDO_POINT_TYPE(3,3, NULL), NULL, NULL), 'mask=ANYINTERACT') = 'TRUE';
-- tak

--2a
SELECT M1.CITY_NAME AS MIASTO, SDO_NN_DISTANCE(1) AS ODL
FROM MAJOR_CITIES M1, MAJOR_CITIES M2
WHERE SDO_NN(M1.GEOM, M2.GEOM, 'sdo_num_res=10 unit=km', 1) = 'TRUE' AND M2.CITY_NAME = 'Warsaw' AND M1.CITY_NAME != 'Warsaw'
ORDER BY ODL DESC;

--2b
SELECT M2.CITY_NAME AS MIASTO
FROM MAJOR_CITIES M1, MAJOR_CITIES M2
WHERE SDO_WITHIN_DISTANCE(M1.GEOM, M2.GEOM, 'distance=100 unit=km') = 'TRUE' AND M1.CITY_NAME = 'Warsaw' AND M2.CITY_NAME != 'Warsaw';

--2c
SELECT M.CNTRY_NAME AS KRAJ, M.CITY_NAME AS MIASTO
FROM MAJOR_CITIES M, COUNTRY_BOUNDARIES C
WHERE SDO_RELATE(M.GEOM, C.GEOM, 'mask=INSIDE') = 'TRUE' AND C.CNTRY_NAME = 'Slovakia';

--2d
SELECT C2.CNTRY_NAME AS PANSTWO, SDO_GEOM.SDO_DISTANCE(C1.GEOM, C2.GEOM, 1, 'unit=km') AS ODL
FROM COUNTRY_BOUNDARIES C1, COUNTRY_BOUNDARIES C2
WHERE SDO_RELATE(C1.GEOM, C2.GEOM, 'mask=ANYINTERACT') != 'TRUE' AND C1.CNTRY_NAME = 'Poland'
ORDER BY ODL DESC;

--3a
SELECT C2.CNTRY_NAME, SDO_GEOM.SDO_LENGTH(SDO_GEOM.SDO_INTERSECTION(C1.GEOM, C2.GEOM, 1), 1, 'unit=km') AS ODLEGLOSC
FROM COUNTRY_BOUNDARIES C1, COUNTRY_BOUNDARIES C2
WHERE SDO_FILTER(C1.GEOM, C2.GEOM) = 'TRUE' AND C1.CNTRY_NAME = 'Poland' AND C2.CNTRY_NAME != 'Poland';

--3b
SELECT *
FROM (
    SELECT CNTRY_NAME
    FROM COUNTRY_BOUNDARIES
    ORDER BY SDO_GEOM.SDO_AREA(GEOM, 1, 'unit=SQ_KM') DESC
)
WHERE ROWNUM = 1;

--3c
SELECT SDO_GEOM.SDO_AREA(SDO_GEOM.SDO_MBR(SDO_GEOM.SDO_UNION(M1.GEOM, M2.GEOM, 1)), 1, 'unit=SQ_KM') AS SQ_KM
FROM MAJOR_CITIES M1, MAJOR_CITIES M2
WHERE M1.CITY_NAME = 'Warsaw' AND M2.CITY_NAME = 'Lodz';

--3d
SELECT SDO_GEOM.SDO_UNION(C.GEOM, M.GEOM, 1).GET_GTYPE() AS GTYPE
FROM MAJOR_CITIES M, COUNTRY_BOUNDARIES C
WHERE M.CITY_NAME = 'Prague' AND C.CNTRY_NAME = 'Poland';

--3e
SELECT R.CITY_NAME, R.CNTRY_NAME
FROM (
    SELECT C.CNTRY_NAME, M.CITY_NAME, SDO_GEOM.SDO_DISTANCE(M.GEOM, SDO_GEOM.SDO_CENTROID(C.GEOM, 1), 1) AS DIST
    FROM COUNTRY_BOUNDARIES C, MAJOR_CITIES M
    WHERE M.CNTRY_NAME = C.CNTRY_NAME AND C.CNTRY_NAME = M.CNTRY_NAME
    ORDER BY DIST
) R
WHERE ROWNUM = 1;

--3f
SELECT R.NAME, SUM(SDO_GEOM.SDO_LENGTH(SDO_GEOM.SDO_INTERSECTION(R.GEOM, C.GEOM, 1), 1, 'unit=km')) AS DLUGOSC
FROM RIVERS R, COUNTRY_BOUNDARIES C
WHERE SDO_RELATE(R.GEOM, C.GEOM, 'mask=ANYINTERACT') = 'TRUE' AND C.CNTRY_NAME = 'Poland'
GROUP BY R.NAME
ORDER BY DLUGOSC DESC;