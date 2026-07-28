module annotated.type.argument {
    uses example.Service;
    provides example.Service with example.ServiceImpl, example.AlternativeServiceImpl;
}
