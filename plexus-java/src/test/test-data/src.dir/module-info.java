/**
 * Some javadoc
 */
open module a.b.c 
{
    requires d.e;
    requires static s.d.e;
    requires transitive t.d.e;
    requires static transitive s.t.d.e;
    
    exports f.g;
    exports f.g.h to i.j, k.l.m;
    
    uses com.example.foo.spi.Intf;
    provides com.example.foo.spi.Intf with com.example.foo.Impl;
}