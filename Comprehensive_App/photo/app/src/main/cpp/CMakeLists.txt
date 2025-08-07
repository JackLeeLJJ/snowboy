cmake_minimum_required(VERSION 3.18.1)

project("frontcamera")

# 设置C++标准
set(CMAKE_CXX_STANDARD 11)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# 强制使用libc++而不是libstdc++
set(CMAKE_CXX_FLAGS "${CMAKE_CXX_FLAGS} -stdlib=libc++")
set(CMAKE_EXE_LINKER_FLAGS "${CMAKE_EXE_LINKER_FLAGS} -stdlib=libc++")
set(CMAKE_SHARED_LINKER_FLAGS "${CMAKE_SHARED_LINKER_FLAGS} -stdlib=libc++")

# 添加头文件路径
include_directories(${CMAKE_CURRENT_SOURCE_DIR}/include)

# 设置Snowboy库路径
set(snowboy_lib_path "${CMAKE_CURRENT_SOURCE_DIR}/../jniLibs/${ANDROID_ABI}/libsnowboy-detect.a")

# 暂时禁用真实Snowboy库，使用模拟实现
# 检查Snowboy库是否存在
if(FALSE AND EXISTS ${snowboy_lib_path})
    message(STATUS "Found Snowboy library: ${snowboy_lib_path}")
    # 定义宏以启用真实Snowboy功能
    add_definitions(-DSNOWBOY_REAL)
else()
    message(WARNING "Using mock Snowboy implementation (real library temporarily disabled)")
endif()

# 创建共享库
add_library(snowboy-detect-android SHARED
    snowboy-wrapper.cpp
)

# 设置编译标志
target_compile_options(snowboy-detect-android PRIVATE
    -O3
    -fPIC
    -Wall
    -Wextra
    -stdlib=libc++
)

# 链接系统库和Snowboy库
if(FALSE AND EXISTS ${snowboy_lib_path})
    # 尝试链接真实的Snowboy库，添加C++标准库支持
    target_link_libraries(snowboy-detect-android
        log
        ${snowboy_lib_path}
        c++_static
        c++abi
        android
    )
    message(STATUS "Linking with real Snowboy library")
else()
    target_link_libraries(snowboy-detect-android
        log
    )
    message(STATUS "Using mock Snowboy implementation")
endif() 