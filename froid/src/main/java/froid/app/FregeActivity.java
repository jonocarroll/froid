package froid.app;

/**
 * The base Activity that froid apps subclass (via `native module type Activity`).
 * It bridges Android's onCreate to the app's Frege `onCreate :: Activity -> IO ()`
 * — so app code never writes a native-module block. This is plain Java and lives
 * in the library precisely so it can be compiled before the Frege compiler runs
 * (Frege can't see classes generated within its own build pass).
 */
public class FregeActivity extends android.app.Activity {

    @Override
    public void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        try {
            java.lang.reflect.Method onCreate = null;
            for (java.lang.reflect.Method m : this.getClass().getDeclaredMethods()) {
                if (m.getName().equals("onCreate") && m.getParameterCount() == 1) {
                    onCreate = m;
                    break;
                }
            }
            if (onCreate == null) {
                android.util.Log.e("froid",
                    "No `onCreate :: Activity -> IO ()` found in " + this.getClass().getName());
                return;
            }
            Object arg = onCreate.getParameterTypes()[0].isAssignableFrom(android.app.Activity.class)
                ? this : frege.run8.Thunk.lazy(this);
            Object io = onCreate.invoke(null, arg);
            if (io instanceof frege.run8.Lazy) {
                io = ((frege.run8.Lazy<?>) io).call();
            }
            @SuppressWarnings("unchecked")
            frege.run8.Func.U<frege.runtime.Phantom.RealWorld, ?> action =
                (frege.run8.Func.U<frege.runtime.Phantom.RealWorld, ?>) io;
            frege.prelude.PreludeBase.TST.performUnsafe(action).call();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
