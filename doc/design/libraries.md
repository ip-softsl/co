# Libraries

| Name                     | Description |
|--------------------------|-------------|
| *libaether_cli*         | RPC client functionality used by *aether-cli* executable |
| *libaether_common*      | Home for common functionality shared by different executables and libraries. Similar to *libaether_util*, but higher-level (see [Dependencies](#dependencies)). |
| *libaether_consensus*   | Stable, backwards-compatible consensus functionality used by *libaether_node* and *libaether_wallet* and also exposed as a [shared library](../shared-libraries.md). |
| *libaetherconsensus*    | Shared library build of static *libaether_consensus* library |
| *libaether_kernel*      | Consensus engine and support library used for validation by *libaether_node* and also exposed as a [shared library](../shared-libraries.md). |
| *libaetherqt*           | GUI functionality used by *aether-qt* and *aether-gui* executables |
| *libaether_ipc*         | IPC functionality used by *aether-node*, *aether-wallet*, *aether-gui* executables to communicate when [`--enable-multiprocess`](multiprocess.md) is used. |
| *libaether_node*        | P2P and RPC server functionality used by *aetherd* and *aether-qt* executables. |
| *libaether_util*        | Home for common functionality shared by different executables and libraries. Similar to *libaether_common*, but lower-level (see [Dependencies](#dependencies)). |
| *libaether_wallet*      | Wallet functionality used by *aetherd* and *aether-wallet* executables. |
| *libaether_wallet_tool* | Lower-level wallet functionality used by *aether-wallet* executable. |
| *libaether_zmq*         | [ZeroMQ](../zmq.md) functionality used by *aetherd* and *aether-qt* executables. |

## Conventions

- Most libraries are internal libraries and have APIs which are completely unstable! There are few or no restrictions on backwards compatibility or rules about external dependencies. Exceptions are *libaether_consensus* and *libaether_kernel* which have external interfaces documented at [../shared-libraries.md](../shared-libraries.md).

- Generally each library should have a corresponding source directory and namespace. Source code organization is a work in progress, so it is true that some namespaces are applied inconsistently, and if you look at [`libaether_*_SOURCES`](../../src/Makefile.am) lists you can see that many libraries pull in files from outside their source directory. But when working with libraries, it is good to follow a consistent pattern like:

  - *libaether_node* code lives in `src/node/` in the `node::` namespace
  - *libaether_wallet* code lives in `src/wallet/` in the `wallet::` namespace
  - *libaether_ipc* code lives in `src/ipc/` in the `ipc::` namespace
  - *libaether_util* code lives in `src/util/` in the `util::` namespace
  - *libaether_consensus* code lives in `src/consensus/` in the `Consensus::` namespace

## Dependencies

- Libraries should minimize what other libraries they depend on, and only reference symbols following the arrows shown in the dependency graph below:

<table><tr><td>

```mermaid

%%{ init : { "flowchart" : { "curve" : "basis" }}}%%

graph TD;

aether-cli[aether-cli]-->libaether_cli;

aetherd[aetherd]-->libaether_node;
aetherd[aetherd]-->libaether_wallet;

aether-qt[aether-qt]-->libaether_node;
aether-qt[aether-qt]-->libaetherqt;
aether-qt[aether-qt]-->libaether_wallet;

aether-wallet[aether-wallet]-->libaether_wallet;
aether-wallet[aether-wallet]-->libaether_wallet_tool;

libaether_cli-->libaether_util;
libaether_cli-->libaether_common;

libaether_common-->libaether_consensus;
libaether_common-->libaether_util;

libaether_kernel-->libaether_consensus;
libaether_kernel-->libaether_util;

libaether_node-->libaether_consensus;
libaether_node-->libaether_kernel;
libaether_node-->libaether_common;
libaether_node-->libaether_util;

libaetherqt-->libaether_common;
libaetherqt-->libaether_util;

libaether_wallet-->libaether_common;
libaether_wallet-->libaether_util;

libaether_wallet_tool-->libaether_wallet;
libaether_wallet_tool-->libaether_util;

classDef bold stroke-width:2px, font-weight:bold, font-size: smaller;
class aether-qt,aetherd,aether-cli,aether-wallet bold
```
</td></tr><tr><td>

**Dependency graph**. Arrows show linker symbol dependencies. *Consensus* lib depends on nothing. *Util* lib is depended on by everything. *Kernel* lib depends only on consensus and util.

</td></tr></table>

- The graph shows what _linker symbols_ (functions and variables) from each library other libraries can call and reference directly, but it is not a call graph. For example, there is no arrow connecting *libaether_wallet* and *libaether_node* libraries, because these libraries are intended to be modular and not depend on each other's internal implementation details. But wallet code is still able to call node code indirectly through the `interfaces::Chain` abstract class in [`interfaces/chain.h`](../../src/interfaces/chain.h) and node code calls wallet code through the `interfaces::ChainClient` and `interfaces::Chain::Notifications` abstract classes in the same file. In general, defining abstract classes in [`src/interfaces/`](../../src/interfaces/) can be a convenient way of avoiding unwanted direct dependencies or circular dependencies between libraries.

- *libaether_consensus* should be a standalone dependency that any library can depend on, and it should not depend on any other libraries itself.

- *libaether_util* should also be a standalone dependency that any library can depend on, and it should not depend on other internal libraries.

- *libaether_common* should serve a similar function as *libaether_util* and be a place for miscellaneous code used by various daemon, GUI, and CLI applications and libraries to live. It should not depend on anything other than *libaether_util* and *libaether_consensus*. The boundary between _util_ and _common_ is a little fuzzy but historically _util_ has been used for more generic, lower-level things like parsing hex, and _common_ has been used for aether-specific, higher-level things like parsing base58. The difference between util and common is mostly important because *libaether_kernel* is not supposed to depend on *libaether_common*, only *libaether_util*. In general, if it is ever unclear whether it is better to add code to *util* or *common*, it is probably better to add it to *common* unless it is very generically useful or useful particularly to include in the kernel.


- *libaether_kernel* should only depend on *libaether_util* and *libaether_consensus*.

- The only thing that should depend on *libaether_kernel* internally should be *libaether_node*. GUI and wallet libraries *libaetherqt* and *libaether_wallet* in particular should not depend on *libaether_kernel* and the unneeded functionality it would pull in, like block validation. To the extent that GUI and wallet code need scripting and signing functionality, they should be get able it from *libaether_consensus*, *libaether_common*, and *libaether_util*, instead of *libaether_kernel*.

- GUI, node, and wallet code internal implementations should all be independent of each other, and the *libaetherqt*, *libaether_node*, *libaether_wallet* libraries should never reference each other's symbols. They should only call each other through [`src/interfaces/`](`../../src/interfaces/`) abstract interfaces.

## Work in progress

- Validation code is moving from *libaether_node* to *libaether_kernel* as part of [The libaetherkernel Project #24303](https://github.com/aether/aether/issues/24303)
- Source code organization is discussed in general in [Library source code organization #15732](https://github.com/aether/aether/issues/15732)
