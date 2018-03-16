/**
 * Some javadoc
 */
open module a.b.c 
{
    requires d.e;
    requires static s.d.e;
    
    exports f.g;
    exports f.g.h to i.j, k.l.m;
}