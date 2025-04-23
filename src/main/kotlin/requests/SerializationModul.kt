package org.example.requests


import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.polymorphic


val requestModule = SerializersModule {
    polymorphic(CommandRequestInterface::class) {
        subclass(NoArgs::class, NoArgs.serializer())
        subclass(IdRequest::class, IdRequest.serializer())
        subclass(AddRequest::class, AddRequest.serializer())
        subclass(AddIfMaxRequest::class, AddIfMaxRequest.serializer())
        subclass(AddIfMinRequest::class, AddIfMinRequest.serializer())
        subclass(UpdateIdRequest::class, UpdateIdRequest.serializer())
        subclass(FilterByEnginePowerRequest::class, FilterByEnginePowerRequest.serializer())
    }
}
