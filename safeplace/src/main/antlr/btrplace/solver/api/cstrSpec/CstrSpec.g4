grammar CstrSpec;

BLOCK_COMMENT
: '/*' .*? '*/' -> channel(HIDDEN)
;

LINE_COMMENT
: '//' ~[\r\n]* -> channel(HIDDEN)
;
WS
: [ \t\r\n\f]+
-> channel(HIDDEN)
;

LACC:'{';
RACC:'}';
COMMA:',';
IN:':';
NOT_IN:'/:';
INCL:'<:';  
NOT_INCL:'/<:';
PLUS:'+';
MINUS:'-';
MULT:'*';
DIV:'/';
ALL:'!';
EXISTS:'?';
INT: '0' | '-'?[1..9][0..9]*;
ID: [a-zA-Z_][a-zA-Z0-9_]*;
INTER: '\\/';
UNION: '/\\';
AND:'&';
OR:'|';
EQ:'=';
NOT_EQ:'/=';
LPARA :'(';
RPARA:')';
DEF_CONTENT: '::=';
IMPLIES:'-->';
IFF: '<-->';
LT:'<';
LEQ:'<=';
GT:'>';
GEQ:'>=';
TRUE:'true';
FALSE:'false';
NOT:'~';
LBRACK: '[';
RBRACK: ']';
STRING: '"' (~('\\'|'"'))* '"';
END: '$';
BEGIN: '^';

term: t1=term op=(INTER|UNION|PLUS|MINUS|MULT|DIV) t2=term         #termOp
    | LPARA term RPARA                              #protectedTerm
    | call                                         #termFunc
    | ID                               #idTerm    
    | ID  LBRACK term RBRACK                              #arrayTerm    
    | set                                    #setTerm
    | list      #listTerm
    | INT                                           #intTerm
    | STRING #stringTerm
    ;

set: LACC term '.' typedef (COMMA formula)? RACC #setInComprehension
   | LACC term (COMMA term)* RACC #setInExtension;

list: LBRACK term '.' typedef (COMMA formula)? RBRACK #listInComprehension
      | LBRACK term (COMMA term)* RBRACK #listInExtension;

comparison: t1=term op=(EQ | NOT_EQ| LT | LEQ | GT | GEQ | IN | NOT_IN | INCL | NOT_INCL) t2=term;
typedef: ID (COMMA ID)* op=(IN|INCL|NOT_IN|NOT_INCL) i2=term;
formula: LPARA formula RPARA   #protectedFormula
       |f1=formula op=(IMPLIES|OR|AND|IFF) f2=formula              #formulaOp
       |comparison #termComparison
       |NOT formula     #not
       |ALL LPARA typedef RPARA formula #all
       |EXISTS LPARA typedef RPARA formula #exists
       |TRUE        #trueFormula
       |FALSE       #falseFormula
       |call        #cstrCall
       ;
       
call: cur=(BEGIN|END)? ID LPARA term (COMMA term)* RPARA;

constraint: 'cstr' ID LPARA (typedef (COMMA typedef)*)? RPARA DEF_CONTENT formula;
invariant: 'inv' ID DEF_CONTENT formula;

spec: invariant* constraint*;